package com.hit.spring.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ScheduledExecutorService;

/** Creates named executors; callers own their shutdown lifecycle. */
public class ExecutorServiceFactory {
    private final boolean virtualThreads;

    public ExecutorServiceFactory(boolean virtualThreads) {
        this.virtualThreads = virtualThreads;
    }

    public ExecutorService newSingleThreadExecutor(String namePrefix) {
        return newSingleThreadExecutor(namePrefix, false);
    }

    public ExecutorService newSingleThreadExecutor(String namePrefix, boolean daemon) {
        return Executors.newSingleThreadExecutor(threadFactory(namePrefix, daemon));
    }

    public ScheduledExecutorService newSingleThreadScheduledExecutor(String namePrefix, boolean daemon) {
        return Executors.newSingleThreadScheduledExecutor(threadFactory(namePrefix, daemon));
    }

    public ExecutorService newThreadPerTaskExecutor(String namePrefix) {
        return Executors.newThreadPerTaskExecutor(threadFactory(namePrefix, false));
    }

    public ExecutorService newFixedThreadPool(int parallelism, String namePrefix) {
        return Executors.newFixedThreadPool(parallelism, threadFactory(namePrefix, false));
    }

    private ThreadFactory threadFactory(String namePrefix, boolean daemon) {
        return virtualThreads
                ? Thread.ofVirtual().name(namePrefix, 0).factory()
                : Thread.ofPlatform().daemon(daemon).name(namePrefix, 0).factory();
    }
}
