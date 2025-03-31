package com.hit.spring.core.manager;

import com.hit.spring.context.SecurityContext;
import com.hit.spring.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static com.hit.spring.core.constants.enums.TrackingContextEnum.CORRELATION_ID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = {"app.task.executor.enable"},
        havingValue = "true"
)
public class ExecutorManager {

    @Qualifier(value = "threadPoolTaskExecutor")
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void runTask(Runnable runnable) {
        threadPoolTaskExecutor.execute(new WrappedTask(runnable));
    }

    public <T> Future<T> runCallable(Callable<T> callable) {
        return threadPoolTaskExecutor.submit(new WrappedCallable<>(callable));
    }

    public void zipTasksIgnoreException(Runnable... runnables) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Arrays.stream(runnables).forEach(runnable -> {
            CompletableFuture<Void> future = this.runCompletable(runnable)
                    .exceptionally(ex -> {
                        log.error("runCompletable error: {}", ex.getMessage(), ex);
                        return null;
                    });
            futures.add(future);
        });
        this.joinCompletableFutures(futures);
    }

    /**
     * @param items    Cac item can run task
     * @param function Func thuc hien logic run tung task
     * @param <T>      Kieu du lieu item
     *                 Example: executorManager.zipTasksIgnoreException(
     *                 List.of(1, 2, 3),
     *                 number -> {
     *                 if (number == 1) {
     *                 throw new RuntimeException("exception");
     *                 }
     *                 System.out.println(num);
     *                 // logic for number here
     *                 }
     *                 );
     */
    public <T> void zipTasksIgnoreException(List<T> items, Consumer<T> function) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        items.forEach(item -> {
            CompletableFuture<Void> future = this.runCompletable(() ->
                    function.accept(item)
            ).exceptionally(ex -> {
                log.error("runCompletable error: {}", ex.getMessage(), ex);
                return null;
            });
            futures.add(future);
        });
        this.joinCompletableFutures(futures);
    }

    private CompletableFuture<Void> runCompletable(Runnable runnable) {
        return CompletableFuture.runAsync(new WrappedTask(runnable), threadPoolTaskExecutor);
    }

    private void joinCompletableFutures(List<CompletableFuture<Void>> cfs) {
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(cfs.toArray(new CompletableFuture[0]));
        allFutures.join();
    }

    private static class WrappedCallable<T> implements Callable<T> {

        private final Callable<T> task;
        private final String correlationId;

        private WrappedCallable(Callable<T> task) {
            this.task = task;
            this.correlationId = ThreadContext.get(CORRELATION_ID.getKey());
        }

        @Override
        public T call() {
            try {
                ThreadContext.put(CORRELATION_ID.getKey(), correlationId);
                return task.call();
            } catch (Exception e) {
                throw new BusinessException(e.getMessage(), e);
            } finally {
                ThreadContext.clearAll();
                SecurityContext.clearContext();
            }
        }
    }

    private static class WrappedTask implements Runnable {

        private final Runnable task;
        private final String correlationId;

        WrappedTask(Runnable task) {
            this.task = task;
            this.correlationId = ThreadContext.get(CORRELATION_ID.getKey());
        }

        @Override
        public void run() {
            try {
                ThreadContext.put(CORRELATION_ID.getKey(), correlationId);
                task.run();
            } catch (Exception e) {
                throw new BusinessException(e.getMessage(), e);
            } finally {
                ThreadContext.clearAll();
                SecurityContext.clearContext();
            }
        }
    }
}
