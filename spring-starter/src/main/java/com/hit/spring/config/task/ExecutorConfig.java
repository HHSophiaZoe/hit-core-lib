package com.hit.spring.config.task;

import com.hit.spring.config.condition.annotation.ConditionalOnAppExecutorEnable;
import com.hit.spring.config.properties.TaskExecutorProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ExecutorConfig {

    @Bean(name = {"appTaskExecutor"})
    @ConditionalOnAppExecutorEnable
    @ConditionalOnThreading(Threading.PLATFORM)
    public ThreadPoolTaskExecutor appTaskExecutor(TaskExecutorProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setCorePoolSize(properties.getPool().getCoreSize());
        executor.setMaxPoolSize(properties.getPool().getMaxSize());
        executor.setQueueCapacity(properties.getPool().getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getPool().getKeepAliveSeconds());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    @Bean(name = {"appTaskExecutor"})
    @ConditionalOnAppExecutorEnable
    @ConditionalOnThreading(Threading.VIRTUAL)
    public SimpleAsyncTaskExecutor appTaskExecutorVirtualThreads(TaskExecutorProperties properties) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        if (Objects.nonNull(properties.getSimple().getConcurrencyLimit())) {
            executor.setConcurrencyLimit(properties.getSimple().getConcurrencyLimit());
        }
        return executor;
    }

}
