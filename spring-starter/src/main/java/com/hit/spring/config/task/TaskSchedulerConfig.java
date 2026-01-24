package com.hit.spring.config.task;

import com.hit.spring.core.wrapper.RunnableWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerCustomizer;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskSchedulerConfig {

    @Bean
    @ConditionalOnThreading(Threading.PLATFORM)
    public ThreadPoolTaskSchedulerCustomizer threadPoolTaskSchedulerCustomizer() {
        return scheduler -> {
            scheduler.setTaskDecorator(RunnableWrapper::new);
        };
    }

    @Bean
    @ConditionalOnThreading(Threading.VIRTUAL)
    public SimpleAsyncTaskSchedulerCustomizer simpleAsyncTaskExecutorCustomizer() {
        return scheduler -> {
            scheduler.setTaskDecorator(RunnableWrapper::new);
        };
    }

}
