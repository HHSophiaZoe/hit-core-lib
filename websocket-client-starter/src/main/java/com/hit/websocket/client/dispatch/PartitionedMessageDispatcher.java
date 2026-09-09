package com.hit.websocket.client.dispatch;

import lombok.extern.slf4j.Slf4j;
import com.hit.websocket.client.connection.ConnectionId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** One bounded, single-worker executor per partition; JDK owns worker lifecycle and queue wakeups. */
@Slf4j
final class PartitionedMessageDispatcher implements MessageDispatcher {
    private final ConnectionId connectionId;
    private final MessageDispatcherOptions options;
    private final List<MessageDispatcherObserver> observers;
    private final List<Partition> partitions = new ArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    PartitionedMessageDispatcher(ConnectionId connectionId, MessageDispatcherOptions options,
                                 List<MessageDispatcherObserver> observers, ThreadFactory threads) {
        this.connectionId = Objects.requireNonNull(connectionId, "connectionId");
        this.options = Objects.requireNonNull(options);
        this.observers = List.copyOf(observers);
        for (int partition = 0; partition < options.partitions(); partition++) {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(options.queueCapacity()), threads);
            partitions.add(new Partition(executor));
            registerQueue(partition, executor);
        }
    }

    @Override
    public void dispatch(String metricCategory, String orderingKey, Runnable handler) {
        Objects.requireNonNull(handler, "handler");
        if (orderingKey == null || orderingKey.isBlank()) throw new IllegalArgumentException("orderingKey cannot be blank");
        String category = metricCategory == null || metricCategory.isBlank() ? UNSPECIFIED_CATEGORY : metricCategory;
        int partition = Math.floorMod(orderingKey.hashCode(), partitions.size());
        DispatchTask task = new DispatchTask(new DispatchContext(connectionId, options.name(), category, partition), handler);
        DispatchTask discarded = partitions.get(partition).submit(task);
        if (discarded != null) dropped(discarded);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        for (Partition partition : partitions) {
            partition.stop().forEach(task -> dropped((DispatchTask) task));
        }
    }

    /** Serialize producers and shutdown only. Neither handlers nor observers run under this lock. */
    private final class Partition {
        private final ThreadPoolExecutor executor;
        private final ReentrantLock submissionLock = new ReentrantLock();

        private Partition(ThreadPoolExecutor executor) {
            this.executor = executor;
        }

        private DispatchTask submit(DispatchTask task) {
            submissionLock.lock();
            try {
                if (closed.get()) throw new IllegalStateException("Message dispatcher is closed");
                try {
                    executor.execute(task);
                    return null;
                } catch (RejectedExecutionException error) {
                    return switch (options.overflowPolicy()) {
                        case FAIL -> throw error;
                        case DROP_LATEST -> task;
                        case DROP_OLDEST -> {
                            DispatchTask removed = (DispatchTask) executor.getQueue().poll();
                            // Other producers and shutdown cannot steal this slot; workers only free slots.
                            executor.execute(task);
                            yield removed;
                        }
                    };
                }
            } finally {
                submissionLock.unlock();
            }
        }

        private List<Runnable> stop() {
            submissionLock.lock();
            try {
                List<Runnable> queued = new ArrayList<>();
                executor.getQueue().drainTo(queued);
                executor.shutdown();
                return queued;
            } finally {
                submissionLock.unlock();
            }
        }
    }

    private void registerQueue(int partition, ThreadPoolExecutor executor) {
        observe(observer -> observer.queueRegistered(connectionId, options.name(), partition, executor.getQueue()::size));
    }

    private void dropped(DispatchTask task) {
        observe(observer -> observer.dropped(task.context));
    }

    private void observe(Consumer<MessageDispatcherObserver> notification) {
        for (MessageDispatcherObserver observer : observers) {
            try {
                notification.accept(observer);
            } catch (RuntimeException error) {
                log.warn("Dispatcher observer failed for {}", options.name(), error);
            }
        }
    }

    private final class DispatchTask implements Runnable {
        private final DispatchContext context;
        private final Runnable handler;

        private DispatchTask(DispatchContext context, Runnable handler) {
            this.context = context;
            this.handler = handler;
        }

        @Override
        public void run() {
            long started = System.nanoTime();
            try {
                handler.run();
                observe(observer -> observer.processed(context, Duration.ofNanos(System.nanoTime() - started)));
            } catch (RuntimeException error) {
                log.error("Message dispatch failed for {}", context, error);
                observe(observer -> observer.failed(context, error));
            }
        }
    }
}
