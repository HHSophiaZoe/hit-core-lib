package com.hit.spring.config.task;

import com.hit.spring.config.condition.annotation.ConditionalOnAppExecutorEnable;
import com.hit.spring.config.properties.TaskExecutorProperties;
import com.hit.spring.core.wrapper.RunnableWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@ConditionalOnAppExecutorEnable
public class TaskExecutorConfig {

    @EnableAsync
    @Configuration
    public static class AsyncConfigurerSupport implements AsyncConfigurer {

        private final TaskExecutor taskExecutor;

        public AsyncConfigurerSupport(@Qualifier("appTaskExecutor") TaskExecutor taskExecutor) {
            this.taskExecutor = taskExecutor;
        }

        @Override
        public Executor getAsyncExecutor() {
            return taskExecutor;
        }

        @Override
        public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
            return (ex, method, params) -> {
                log.error("@Async execute error | method={} | params={}", method.toGenericString(), Arrays.toString(params), ex);
            };
        }
    }

    @Primary
    @Bean(name = {"appTaskExecutor"})
    @ConditionalOnThreading(Threading.PLATFORM)
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(TaskExecutorProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(RunnableWrapper::new);
        executor.setThreadNamePrefix(properties.getPool().getThreadNamePrefix());
        executor.setCorePoolSize(properties.getPool().getCoreSize());
        executor.setMaxPoolSize(properties.getPool().getMaxSize());
        executor.setQueueCapacity(properties.getPool().getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getPool().getKeepAliveSeconds());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    @Primary
    @Bean(name = {"appTaskExecutor"})
    @ConditionalOnThreading(Threading.VIRTUAL)
    public SimpleAsyncTaskExecutor simpleAsyncTaskExecutor(TaskExecutorProperties properties) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(RunnableWrapper::new);
        executor.setThreadNamePrefix(properties.getSimple().getThreadNamePrefix());
        executor.setConcurrencyLimit(properties.getSimple().getConcurrencyLimit());
        return executor;
    }

}
