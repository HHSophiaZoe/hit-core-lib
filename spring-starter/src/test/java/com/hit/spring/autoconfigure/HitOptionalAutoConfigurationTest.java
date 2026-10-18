package com.hit.spring.autoconfigure;

import com.hit.spring.excel.IExcelService;
import com.hit.spring.excel.CellStyleCreatorFactory;
import com.hit.spring.http.HttpService;
import com.hit.spring.task.ExecutorManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerCustomizer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import com.hit.spring.tracing.TrackingContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HitOptionalAutoConfigurationTest {
    private final ApplicationContextRunner http = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, HitHttpClientAutoConfiguration.class));

    @Test
    void httpRequiresExplicitEnable() {
        http.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(HttpService.class)
                .doesNotHaveBean(RestClient.class).doesNotHaveBean(HttpComponentsClientHttpRequestFactory.class));
        http.withPropertyValues("http-client.default.enable=false").run(context ->
                assertThat(context).hasNotFailed().doesNotHaveBean(HttpService.class).doesNotHaveBean(RestClient.class));
    }

    @Test
    void enabledHttpIsCreatedOnlyWhenRequested() {
        http.withPropertyValues("http-client.default.enable=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeanFactory().containsSingleton("defaultClientHttpRequestFactory")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("defaultRestClient")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("httpService")).isFalse();
            assertThat(context.getBean(HttpService.class)).isNotNull();
            assertThat(context.getBeanFactory().containsSingleton("defaultClientHttpRequestFactory")).isTrue();
        });
    }

    @Test
    void customRestClientDoesNotCreateUnusedPool() {
        RestClient client = RestClient.create();
        http.withPropertyValues("http-client.default.enable=true")
                .withBean("defaultRestClient", RestClient.class, () -> client).run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(HttpComponentsClientHttpRequestFactory.class);
                    assertThat(context.getBean(RestClient.class)).isSameAs(client);
                    assertThat(context.getBean(HttpService.class)).isNotNull();
                });
    }

    @Test
    void customServiceLeavesUnusedClientUninitialized() {
        HttpService service = mock(HttpService.class);
        http.withPropertyValues("http-client.default.enable=true")
                .withBean(HttpService.class, () -> service).run(context -> {
                    assertThat(context.getBean(HttpService.class)).isSameAs(service);
                    assertThat(context.getBeanFactory().containsSingleton("defaultRestClient")).isFalse();
                    assertThat(context.getBeanFactory().containsSingleton("defaultClientHttpRequestFactory")).isFalse();
                    assertThat(context.getBean(RestClient.class)).isNotNull();
                });
    }

    @Test
    void customRequestFactoryIsReused() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        http.withPropertyValues("http-client.default.enable=true")
                .withBean("defaultClientHttpRequestFactory", HttpComponentsClientHttpRequestFactory.class, () -> factory)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(HttpComponentsClientHttpRequestFactory.class);
                    assertThat(context.getBean(HttpService.class)).isNotNull();
                    assertThat(context.getBean(HttpComponentsClientHttpRequestFactory.class)).isSameAs(factory);
                });
    }

    @Test
    void customExcelServiceSkipsDefaultStyleFactory() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(HitExcelAutoConfiguration.class))
                .withBean(IExcelService.class, () -> mock(IExcelService.class)).run(context ->
                        assertThat(context).hasNotFailed().hasSingleBean(IExcelService.class)
                                .doesNotHaveBean(CellStyleCreatorFactory.class));
    }

    @Test
    void excelIsLazyButCanBeRequested() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
                HitSpringAutoConfiguration.class, HitExcelAutoConfiguration.class)).run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanFactory().containsSingleton("excelService")).isFalse();
                    assertThat(context.getBeanFactory().containsSingleton("cellStyleCreatorFactory")).isFalse();
                    assertThat(context.getBean(IExcelService.class)).isNotNull();
                });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void executorRequiresEnableAndManagerRemainsLazy(boolean virtualThreads) {
        ApplicationContextRunner tasks = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(HitTaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class))
                .withPropertyValues("spring.threads.virtual.enabled=" + virtualThreads);
        tasks.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(ExecutorManager.class);
            assertThat(context.getBeanFactory().containsSingleton("traceIdThreadPoolTaskSchedulerCustomizer")).isFalse();
        });
        tasks.withPropertyValues("app.task.executor.enable=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeanFactory().containsSingleton("appTaskExecutor")).isTrue();
            assertThat(context.getBeanFactory().containsSingleton("executorManager")).isFalse();
            assertThat(context.getBean(ExecutorManager.class)).isNotNull();
            assertThat(context.getBean(AsyncTaskExecutor.class)
                    .submit(() -> Thread.currentThread().isVirtual()).get(5, TimeUnit.SECONDS)).isEqualTo(virtualThreads);
        });
    }

    @Test
    void applicationExecutorReplacesDefault() {
        AsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(HitTaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class))
                .withPropertyValues("app.task.executor.enable=true")
                .withBean("appTaskExecutor", AsyncTaskExecutor.class, () -> executor).run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(AsyncTaskExecutor.class);
                    assertThat(context.getBean(AsyncTaskExecutor.class)).isSameAs(executor);
                    assertThat(context.getBean(ExecutorManager.class)).isNotNull();
                });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void schedulerCustomizationRequiresSchedulingAndPreservesTrace(boolean virtualThreads) {
        ApplicationContextRunner scheduling = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(HitTaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class))
                .withPropertyValues("spring.threads.virtual.enabled=" + virtualThreads);
        scheduling.run(context -> assertThat(context).hasNotFailed()
                .doesNotHaveBean(ThreadPoolTaskSchedulerCustomizer.class)
                .doesNotHaveBean(SimpleAsyncTaskSchedulerCustomizer.class));
        scheduling.withUserConfiguration(SchedulingEnabled.class).run(context -> {
            assertThat(context).hasNotFailed();
            CompletableFuture<String> trace = new CompletableFuture<>();
            TrackingContext.setTraceId("scheduled-trace");
            try {
                Runnable task = () -> trace.complete(TrackingContext.currentTraceId());
                TaskScheduler scheduler = context.getBean(TaskScheduler.class);
                if (scheduler instanceof AsyncTaskExecutor executor) {
                    executor.execute(task);
                } else {
                    scheduler.schedule(task, Instant.now());
                }
                assertThat(trace.get(5, TimeUnit.SECONDS)).isEqualTo("scheduled-trace");
            } finally {
                TrackingContext.clearContext();
            }
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingEnabled { }
}
