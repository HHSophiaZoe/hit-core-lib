package com.hit.spring.autoconfigure;

import com.hit.spring.task.TaskExecutorProperties;
import com.hit.spring.task.DefaultExecutorManager;
import com.hit.spring.task.ExecutorManager;
import com.hit.spring.task.RunnableWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerCustomizer;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.config.TaskManagementConfigUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * The application executor {@code appTaskExecutor} (virtual threads or a pool, following
 * {@code spring.threads.virtual.enabled}), {@code @Async} on it, {@link ExecutorManager}, and trace id propagation
 * into scheduled tasks.
 */
@Slf4j
@EnableConfigurationProperties(TaskExecutorProperties.class)
@AutoConfiguration(before = {TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class})
public class HitTaskExecutionAutoConfiguration {

    public static final String APP_TASK_EXECUTOR = "appTaskExecutor";

    @Bean
    @ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
    @ConditionalOnThreading(Threading.PLATFORM)
    public ThreadPoolTaskSchedulerCustomizer traceIdThreadPoolTaskSchedulerCustomizer() {
        return scheduler -> scheduler.setTaskDecorator(RunnableWrapper::new);
    }

    @Bean
    @ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
    @ConditionalOnThreading(Threading.VIRTUAL)
    public SimpleAsyncTaskSchedulerCustomizer traceIdSimpleAsyncTaskSchedulerCustomizer() {
        return scheduler -> scheduler.setTaskDecorator(RunnableWrapper::new);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBooleanProperty("app.task.executor.enable")
    static class AppTaskExecutorConfiguration {

        @Primary
        @Bean(name = APP_TASK_EXECUTOR)
        @ConditionalOnMissingBean(name = APP_TASK_EXECUTOR)
        @ConditionalOnThreading(Threading.PLATFORM)
        public ThreadPoolTaskExecutor threadPoolTaskExecutor(TaskExecutorProperties properties) {
            TaskExecutorProperties.Pool pool = properties.getPool();
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setTaskDecorator(RunnableWrapper::new);
            executor.setThreadNamePrefix(pool.getThreadNamePrefix());
            executor.setCorePoolSize(pool.getCoreSize());
            executor.setMaxPoolSize(pool.getMaxSize());
            executor.setQueueCapacity(pool.getQueueCapacity());
            executor.setKeepAliveSeconds(pool.getKeepAliveSeconds());
            executor.setAllowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout());
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            executor.setWaitForTasksToCompleteOnShutdown(true);
            return executor;
        }

        @Primary
        @Bean(name = APP_TASK_EXECUTOR)
        @ConditionalOnMissingBean(name = APP_TASK_EXECUTOR)
        @ConditionalOnThreading(Threading.VIRTUAL)
        public SimpleAsyncTaskExecutor simpleAsyncTaskExecutor(TaskExecutorProperties properties) {
            SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(properties.getSimple().getThreadNamePrefix());
            executor.setVirtualThreads(true);
            executor.setTaskDecorator(RunnableWrapper::new);
            executor.setConcurrencyLimit(properties.getSimple().getConcurrencyLimit());
            return executor;
        }

        @Lazy
        @Bean
        @ConditionalOnMissingBean(ExecutorManager.class)
        public DefaultExecutorManager executorManager(@Qualifier(APP_TASK_EXECUTOR) AsyncTaskExecutor executor,
                                                      TaskExecutorProperties properties) {
            return new DefaultExecutorManager(executor, Duration.ofSeconds(properties.getTaskTimeoutSeconds()));
        }
    }

    /**
     * {@code @Async} methods run on {@code appTaskExecutor}; uncaught errors are logged.
     */
    @EnableAsync
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBooleanProperty("app.task.executor.enable")
    static class AsyncConfiguration implements AsyncConfigurer {

        // Resolved on first @Async call, so the executor is not created while post-processors are being set up
        private final ObjectProvider<Executor> executor;

        AsyncConfiguration(@Qualifier(APP_TASK_EXECUTOR) ObjectProvider<Executor> executor) {
            this.executor = executor;
        }

        @Override
        public Executor getAsyncExecutor() {
            return executor.getObject();
        }

        @Override
        public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
            return (ex, method, params) -> log.error("@Async execute error | method={} | params={}",
                    method.toGenericString(), Arrays.toString(params), ex);
        }
    }
}
