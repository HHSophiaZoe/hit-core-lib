package com.hit.websocket.client.autoconfigure;

import com.hit.websocket.client.connection.WebSocketClientFactory;
import com.hit.websocket.client.dispatch.MessageDispatcherRegistry;
import com.hit.websocket.client.dispatch.DispatcherSnapshotQuery;
import com.hit.websocket.client.observability.ConnectionSnapshotQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebSocketTransportAutoConfiguration.class,
                    WebSocketConnectionAutoConfiguration.class, WebSocketObservabilityAutoConfiguration.class,
                    WebSocketDispatcherAutoConfiguration.class));

    @Test
    void worksWithoutOptionalMicrometerClasses() {
        context.withClassLoader(new FilteredClassLoader("io.micrometer"))
                .withPropertyValues("websocket-client.observability.enabled=true")
                .run(application -> {
                    assertThat(application).hasNotFailed();
                    assertThat(application).hasSingleBean(ConnectionSnapshotQuery.class);
                    assertThat(application).hasSingleBean(DispatcherSnapshotQuery.class);
                    assertThat(application).hasSingleBean(WebSocketClientFactory.class);
                    assertThat(application).hasSingleBean(MessageDispatcherRegistry.class);
                });
    }

    @Test
    void observabilityIsOptIn() {
        context.run(application -> {
                    assertThat(application).hasNotFailed();
                    assertThat(application).doesNotHaveBean(ConnectionSnapshotQuery.class);
                    assertThat(application).doesNotHaveBean(DispatcherSnapshotQuery.class);
                    assertThat(application).hasSingleBean(WebSocketClientFactory.class);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void dispatcherUsesConfiguredThreadType(boolean virtualThreads) {
        context.withPropertyValues("spring.threads.virtual.enabled=" + virtualThreads)
                .run(application -> {
                    assertThat(application).hasNotFailed();
                    CompletableFuture<Boolean> threadType = new CompletableFuture<>();
                    application.getBean(MessageDispatcherRegistry.class).get(new com.hit.websocket.client.connection.ConnectionId("test", "thread-test"), "thread-test")
                            .dispatch("key", () -> threadType.complete(Thread.currentThread().isVirtual()));
                    assertThat(threadType.get(2, TimeUnit.SECONDS)).isEqualTo(virtualThreads);
                });
    }
}
