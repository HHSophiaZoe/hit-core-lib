package com.hit.websocket.client.dispatch.memory;

import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.dispatch.DispatchContext;
import com.hit.websocket.client.dispatch.DispatcherSnapshotQuery;
import com.hit.websocket.client.dispatch.MessageDispatcherObserver;
import com.hit.websocket.client.dispatch.model.DispatcherPartitionSnapshot;
import com.hit.websocket.client.dispatch.model.DispatcherSnapshot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntSupplier;

public final class InMemoryDispatcherStateRegistry implements MessageDispatcherObserver, DispatcherSnapshotQuery {

    private final Clock clock;
    private final Map<DispatcherId, MutableDispatcher> dispatchers = new ConcurrentHashMap<>();

    public InMemoryDispatcherStateRegistry(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void queueRegistered(ConnectionId connectionId, String dispatcher, int partition, IntSupplier queueSize) {
        state(connectionId, dispatcher).partitions.put(partition, new MutablePartition(partition, queueSize));
    }

    @Override
    public void processed(DispatchContext context, Duration duration) {
        MutableDispatcher dispatcher = state(context.connectionId(), context.dispatcher());
        dispatcher.processed.increment();
        dispatcher.totalProcessingNanos.add(duration.toNanos());
        dispatcher.lastProcessedAt = clock.instant();
        dispatcher.partition(context.partition()).processed.increment();
    }

    @Override
    public void dropped(DispatchContext context) {
        MutableDispatcher dispatcher = state(context.connectionId(), context.dispatcher());
        dispatcher.dropped.increment();
        dispatcher.partition(context.partition()).dropped.increment();
    }

    @Override
    public void failed(DispatchContext context, Throwable error) {
        MutableDispatcher dispatcher = state(context.connectionId(), context.dispatcher());
        dispatcher.errors.increment();
        dispatcher.lastErrorAt = clock.instant();
        dispatcher.partition(context.partition()).errors.increment();
    }

    @Override
    public List<DispatcherSnapshot> findByConnection(ConnectionId connectionId) {
        return dispatchers.entrySet().stream()
                .filter(entry -> entry.getKey().connectionId().equals(connectionId))
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .sorted(Comparator.comparing(DispatcherSnapshot::name))
                .toList();
    }

    @Override
    public List<DispatcherSnapshot> findBySource(String source) {
        return dispatchers.entrySet().stream()
                .filter(entry -> entry.getKey().connectionId().provider().equals(source))
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .sorted(Comparator.comparing(snapshot -> snapshot.connectionId().value()))
                .toList();
    }

    private MutableDispatcher state(ConnectionId connectionId, String dispatcher) {
        return dispatchers.computeIfAbsent(new DispatcherId(connectionId, dispatcher), ignored -> new MutableDispatcher());
    }

    private record DispatcherId(ConnectionId connectionId, String name) {
    }

    private static final class MutableDispatcher {
        private final Map<Integer, MutablePartition> partitions = new ConcurrentHashMap<>();
        private final LongAdder processed = new LongAdder();
        private final LongAdder dropped = new LongAdder();
        private final LongAdder errors = new LongAdder();
        private final LongAdder totalProcessingNanos = new LongAdder();
        private volatile Instant lastProcessedAt;
        private volatile Instant lastErrorAt;

        private MutablePartition partition(int number) {
            return partitions.computeIfAbsent(number, key -> new MutablePartition(key, () -> 0));
        }

        private DispatcherSnapshot snapshot(DispatcherId id) {
            List<DispatcherPartitionSnapshot> partitionSnapshots = partitions.values().stream()
                    .map(MutablePartition::snapshot)
                    .sorted(Comparator.comparingInt(DispatcherPartitionSnapshot::partition))
                    .toList();
            return DispatcherSnapshot.builder()
                    .connectionId(id.connectionId())
                    .name(id.name())
                    .queued(partitionSnapshots.stream().mapToInt(DispatcherPartitionSnapshot::queued).sum())
                    .processed(processed.sum())
                    .dropped(dropped.sum())
                    .errors(errors.sum())
                    .totalProcessingNanos(totalProcessingNanos.sum())
                    .lastProcessedAt(lastProcessedAt)
                    .lastErrorAt(lastErrorAt)
                    .partitions(partitionSnapshots)
                    .build();
        }
    }

    private static final class MutablePartition {
        private final int number;
        private final IntSupplier queueSize;
        private final LongAdder processed = new LongAdder();
        private final LongAdder dropped = new LongAdder();
        private final LongAdder errors = new LongAdder();

        private MutablePartition(int number, IntSupplier queueSize) {
            this.number = number;
            this.queueSize = queueSize;
        }

        private DispatcherPartitionSnapshot snapshot() {
            return DispatcherPartitionSnapshot.builder()
                    .partition(number)
                    .queued(queueSize.getAsInt())
                    .processed(processed.sum())
                    .dropped(dropped.sum())
                    .errors(errors.sum())
                    .build();
        }
    }
}
