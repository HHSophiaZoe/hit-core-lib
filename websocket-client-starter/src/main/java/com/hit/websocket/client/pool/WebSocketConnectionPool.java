package com.hit.websocket.client.pool;

import com.hit.websocket.client.connection.ConnectionId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Capacity-based physical connection allocator. Resources are provider-defined quota units;
 * the pool does not know subscription protocols or payload formats.
 */
public final class WebSocketConnectionPool<R, C extends PooledWebSocketConnection> {

    private final WebSocketConnectionPoolOptions options;
    private final IntFunction<C> connectionFactory;
    private final ExecutorService coordinator;
    private final Map<ConnectionId, ConnectionSlot<R, C>> connections = new LinkedHashMap<>();
    private final Map<R, Assignment> assignments = new LinkedHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private int nextConnectionSequence = 1;

    WebSocketConnectionPool(WebSocketConnectionPoolOptions options, IntFunction<C> connectionFactory, ThreadFactory threads) {
        this.options = Objects.requireNonNull(options, "options cannot be null");
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory cannot be null");
        this.coordinator = Executors.newSingleThreadExecutor(Objects.requireNonNull(threads, "threads cannot be null"));
    }

    public PoolAllocation<R> acquire(Collection<R> requestedResources) {
        List<R> resources = normalize(requestedResources);
        return coordinate(() -> acquireResources(resources));
    }

    public PoolRelease<R> release(Collection<R> requestedResources) {
        List<R> resources = normalize(requestedResources);
        return coordinate(() -> releaseResources(resources));
    }

    public List<R> resources(ConnectionId connectionId) {
        return coordinate(() -> {
            ConnectionSlot<R, C> slot = connections.get(connectionId);
            return slot == null ? List.of() : List.copyOf(slot.resources());
        });
    }

    public C connection(ConnectionId connectionId) {
        return coordinate(() -> {
            ConnectionSlot<R, C> slot = connections.get(connectionId);
            if (slot == null) throw new IllegalArgumentException("Unknown pooled connection: " + connectionId.value());
            return slot.connection();
        });
    }

    public void connect(Collection<ConnectionId> connectionIds) {
        List<C> targets = coordinate(() -> connectionIds.stream().map(this::connectionDirect).toList());
        targets.forEach(PooledWebSocketConnection::connect);
    }

    public void disconnect(Collection<ConnectionId> connectionIds) {
        List<C> targets = coordinate(() -> connectionIds.stream().map(this::connectionDirect).toList());
        targets.forEach(PooledWebSocketConnection::disconnect);
    }

    public boolean isConnected() {
        return coordinate(() -> {
            List<ConnectionSlot<R, C>> active = connections.values().stream()
                    .filter(slot -> !slot.resources().isEmpty())
                    .toList();
            return !active.isEmpty() && active.stream().allMatch(slot -> slot.connection().isConnected());
        });
    }

    public void maintainConnections() {
        List<C> active = coordinate(() -> connections.values().stream()
                .filter(slot -> !slot.resources().isEmpty())
                .map(ConnectionSlot::connection)
                .toList());
        active.forEach(PooledWebSocketConnection::maintain);
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            List<C> currentConnections = coordinate(() -> {
                List<C> result = connections.values().stream().map(ConnectionSlot::connection).toList();
                connections.clear();
                assignments.clear();
                return result;
            });
            currentConnections.forEach(PooledWebSocketConnection::close);
        } finally {
            coordinator.shutdown();
        }
    }

    private PoolAllocation<R> acquireResources(List<R> resources) {
        int additional = (int) resources.stream().filter(resource -> !assignments.containsKey(resource)).count();
        if (assignments.size() + additional > options.maxTotalResources()) {
            throw new IllegalStateException("Connection pool requires %d additional resources, but only %d/%d remain"
                    .formatted(additional, options.maxTotalResources() - assignments.size(), options.maxTotalResources()));
        }
        Map<ConnectionId, List<R>> assigned = new LinkedHashMap<>();
        Map<ConnectionId, List<R>> added = new LinkedHashMap<>();
        List<R> retained = new ArrayList<>();
        List<R> created = new ArrayList<>();
        try {
            for (R resource : resources) {
                Assignment existing = assignments.get(resource);
                if (existing != null) {
                    existing.retain();
                    retained.add(resource);
                    add(assigned, existing.connectionId(), resource);
                    continue;
                }
                ConnectionSlot<R, C> slot = availableConnection();
                assignments.put(resource, new Assignment(slot.connection().connectionId()));
                slot.resources().add(resource);
                created.add(resource);
                add(assigned, slot.connection().connectionId(), resource);
                add(added, slot.connection().connectionId(), resource);
            }
            return new PoolAllocation<>(assigned, added);
        } catch (RuntimeException error) {
            retained.forEach(resource -> assignments.get(resource).release());
            created.forEach(this::removeAssignment);
            throw error;
        }
    }

    private PoolRelease<R> releaseResources(List<R> resources) {
        Map<ConnectionId, List<R>> removed = new LinkedHashMap<>();
        Set<ConnectionId> touched = new LinkedHashSet<>();
        for (R resource : resources) {
            Assignment assignment = assignments.get(resource);
            if (assignment == null || assignment.release()) continue;
            assignments.remove(resource);
            ConnectionSlot<R, C> slot = connections.get(assignment.connectionId());
            if (slot == null) continue;
            slot.resources().remove(resource);
            touched.add(assignment.connectionId());
            add(removed, assignment.connectionId(), resource);
        }
        Set<ConnectionId> unused = touched.stream()
                .filter(connectionId -> connections.get(connectionId).resources().isEmpty())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new PoolRelease<>(removed, unused);
    }

    private ConnectionSlot<R, C> availableConnection() {
        return connections.values().stream()
                .filter(slot -> slot.resources().size() < options.maxResourcesPerConnection())
                .findFirst()
                .orElseGet(this::newConnection);
    }

    private C connectionDirect(ConnectionId connectionId) {
        ConnectionSlot<R, C> slot = connections.get(connectionId);
        if (slot == null) throw new IllegalArgumentException("Unknown pooled connection: " + connectionId.value());
        return slot.connection();
    }

    private ConnectionSlot<R, C> newConnection() {
        C connection = Objects.requireNonNull(connectionFactory.apply(nextConnectionSequence++),
                "connectionFactory returned null");
        ConnectionId connectionId = Objects.requireNonNull(connection.connectionId(), "connectionId cannot be null");
        if (connections.containsKey(connectionId)) {
            throw new IllegalStateException("Duplicate pooled connection: " + connectionId.value());
        }
        ConnectionSlot<R, C> slot = new ConnectionSlot<>(connection, new LinkedHashSet<>());
        connections.put(connectionId, slot);
        return slot;
    }

    private void removeAssignment(R resource) {
        Assignment assignment = assignments.remove(resource);
        if (assignment == null) return;
        ConnectionSlot<R, C> slot = connections.get(assignment.connectionId());
        if (slot != null) slot.resources().remove(resource);
    }

    private static <R> void add(Map<ConnectionId, List<R>> target, ConnectionId connectionId, R resource) {
        target.computeIfAbsent(connectionId, ignored -> new ArrayList<>()).add(resource);
    }

    private static <R> List<R> normalize(Collection<R> resources) {
        if (resources == null) throw new IllegalArgumentException("resources cannot be null");
        if (resources.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("resources cannot contain null");
        }
        List<R> normalized = List.copyOf(new LinkedHashSet<>(resources));
        if (normalized.isEmpty()) throw new IllegalArgumentException("resources cannot be empty");
        return normalized;
    }

    private <T> T coordinate(Supplier<T> action) {
        try {
            return CompletableFuture.supplyAsync(action, coordinator).join();
        } catch (CompletionException error) {
            if (error.getCause() instanceof RuntimeException runtimeException) throw runtimeException;
            throw error;
        }
    }

    private record ConnectionSlot<R, C extends PooledWebSocketConnection>(C connection, Set<R> resources) { }

    private static final class Assignment {
        private final ConnectionId connectionId;
        private int references = 1;

        private Assignment(ConnectionId connectionId) {
            this.connectionId = connectionId;
        }

        private ConnectionId connectionId() { return connectionId; }
        private void retain() { references++; }
        private boolean release() { return --references > 0; }
    }
}
