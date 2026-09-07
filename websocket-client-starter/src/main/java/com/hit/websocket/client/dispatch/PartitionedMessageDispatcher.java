package com.hit.websocket.client.dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

final class PartitionedMessageDispatcher implements MessageDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(PartitionedMessageDispatcher.class);

    private final MessageDispatcherOptions options;
    private final List<MessageDispatcherObserver> observers;
    private final Object lifecycleMonitor = new Object();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final List<BlockingQueue<DispatchTask>> queues = new ArrayList<>();
    private final List<Thread> workers = new ArrayList<>();

    PartitionedMessageDispatcher(MessageDispatcherOptions options, List<MessageDispatcherObserver> observers) {
        this.options = options;
        this.observers = List.copyOf(observers);
        start();
    }

    @Override
    public void dispatch(String metricCategory, String orderingKey, Runnable handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        if (orderingKey == null || orderingKey.isBlank()) throw new IllegalArgumentException("orderingKey cannot be blank");
        String category = metricCategory == null || metricCategory.isBlank() ? UNSPECIFIED_CATEGORY : metricCategory;
        int partition = Math.floorMod(orderingKey.hashCode(), queues.size());
        DispatchTask task = new DispatchTask(new DispatchContext(options.name(), category, partition), handler);
        BlockingQueue<DispatchTask> queue = queues.get(partition);
        synchronized (lifecycleMonitor) {
            if (!running.get()) throw new IllegalStateException("Message dispatcher is closed");
            if (queue.offer(task)) return;
            handleOverflow(queue, task);
        }
    }

    @Override
    public void close() {
        List<DispatchTask> discarded = new ArrayList<>();
        synchronized (lifecycleMonitor) {
            if (!running.compareAndSet(true, false)) return;
            workers.forEach(Thread::interrupt);
            queues.forEach(queue -> queue.drainTo(discarded));
            lifecycleMonitor.notifyAll();
        }
        discarded.forEach(this::dropped);
    }

    private void start() {
        for (int partition = 0; partition < options.partitions(); partition++) {
            BlockingQueue<DispatchTask> queue = new ArrayBlockingQueue<>(options.queueCapacity());
            queues.add(queue);
            int registeredPartition = partition;
            notifyQueueRegistered(registeredPartition, queue);
            Thread worker = new Thread(() -> consume(queue), options.name() + "-dispatcher-" + partition);
            worker.setDaemon(true);
            worker.start();
            workers.add(worker);
        }
    }

    private void consume(BlockingQueue<DispatchTask> queue) {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                DispatchTask task = queue.take();
                synchronized (lifecycleMonitor) {
                    lifecycleMonitor.notifyAll();
                }
                if (!running.get()) {
                    dropped(task);
                    return;
                }
                execute(task);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void execute(DispatchTask task) {
        long startedAt = System.nanoTime();
        try {
            task.handler().run();
            Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
            notifyProcessed(task.context(), duration);
        } catch (RuntimeException error) {
            LOG.error("Message dispatch failed for dispatcher={}, metricCategory={}, partition={}",
                    task.context().dispatcher(), task.context().metricCategory(), task.context().partition(), error);
            notifyFailed(task.context(), error);
        }
    }

    private void handleOverflow(BlockingQueue<DispatchTask> queue, DispatchTask task) {
        switch (options.overflowPolicy()) {
            case BLOCK -> put(queue, task);
            case DROP_LATEST -> dropped(task);
            case DROP_OLDEST -> {
                DispatchTask removed = queue.poll();
                if (removed != null) dropped(removed);
                if (!queue.offer(task)) dropped(task);
            }
            case FAIL -> throw new RejectedExecutionException("Message dispatcher queue is full: " + options.name());
        }
    }

    private void put(BlockingQueue<DispatchTask> queue, DispatchTask task) {
        if (workers.contains(Thread.currentThread())) {
            throw new RejectedExecutionException("A dispatcher worker cannot block on its own dispatcher");
        }
        try {
            while (running.get()) {
                if (queue.offer(task)) return;
                lifecycleMonitor.wait();
            }
            throw new RejectedExecutionException("Message dispatcher closed while waiting for capacity");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Interrupted while waiting for dispatcher capacity", error);
        }
    }

    private void dropped(DispatchTask task) {
        for (MessageDispatcherObserver observer : observers) {
            try {
                observer.dropped(task.context());
            } catch (RuntimeException error) {
                LOG.warn("Message dispatcher observer failed while recording a dropped task", error);
            }
        }
    }

    private void notifyQueueRegistered(int partition, BlockingQueue<DispatchTask> queue) {
        for (MessageDispatcherObserver observer : observers) {
            try {
                observer.queueRegistered(options.name(), partition, queue::size);
            } catch (RuntimeException error) {
                LOG.warn("Message dispatcher observer failed while registering queue metrics", error);
            }
        }
    }

    private void notifyProcessed(DispatchContext context, Duration duration) {
        for (MessageDispatcherObserver observer : observers) {
            try {
                observer.processed(context, duration);
            } catch (RuntimeException error) {
                LOG.warn("Message dispatcher observer failed while recording a processed task", error);
            }
        }
    }

    private void notifyFailed(DispatchContext context, RuntimeException dispatchError) {
        for (MessageDispatcherObserver observer : observers) {
            try {
                observer.failed(context, dispatchError);
            } catch (RuntimeException observerError) {
                LOG.warn("Message dispatcher observer failed while recording a failed task", observerError);
            }
        }
    }

    private record DispatchTask(DispatchContext context, Runnable handler) {
    }
}
