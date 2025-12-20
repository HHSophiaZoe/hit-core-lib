package com.hit.spring.config.task;

import com.hit.spring.config.condition.annotation.ConditionalOnAppExecutorEnable;
import com.hit.spring.config.properties.TaskExecutorProperties;
import com.hit.spring.core.wrapper.RunnableWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
@ConditionalOnAppExecutorEnable
public class ExecutorConfig {

    @Primary
    @Bean(name = {"appTaskExecutor"})
    @ConditionalOnThreading(Threading.PLATFORM)
    public ThreadPoolTaskExecutor appTaskExecutor(TaskExecutorProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(RunnableWrapper::new);
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
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
    public SimpleAsyncTaskExecutor appTaskExecutorVirtualThreads(TaskExecutorProperties properties) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(RunnableWrapper::new);
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        if (Objects.nonNull(properties.getSimple().getConcurrencyLimit())) {
            executor.setConcurrencyLimit(properties.getSimple().getConcurrencyLimit());
        }
        return executor;
    }

}
