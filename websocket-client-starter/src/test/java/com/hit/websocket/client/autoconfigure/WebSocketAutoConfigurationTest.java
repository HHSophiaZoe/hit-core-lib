package com.hit.websocket.client.autoconfigure;

import com.hit.websocket.client.connection.WebSocketClientFactory;
import com.hit.websocket.client.connection.ConnectionId;
import com.hit.websocket.client.dispatch.MessageDispatcherRegistry;
import com.hit.websocket.client.dispatch.DispatcherSnapshotQuery;
import com.hit.websocket.client.observability.ConnectionSnapshotQuery;
import com.hit.websocket.client.pool.PooledWebSocketConnection;
import com.hit.websocket.client.pool.WebSocketConnectionPool;
import com.hit.websocket.client.pool.WebSocketConnectionPoolFactory;
import com.hit.websocket.client.pool.WebSocketConnectionPoolOptions;
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
                    WebSocketDispatcherAutoConfiguration.class, WebSocketConnectionPoolAutoConfiguration.class));

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
                    assertThat(application).hasSingleBean(WebSocketConnectionPoolFactory.class);
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
    void managedWorkersUseConfiguredThreadType(boolean virtualThreads) {
        context.withPropertyValues("spring.threads.virtual.enabled=" + virtualThreads)
                .run(application -> {
                    assertThat(application).hasNotFailed();
                    CompletableFuture<Boolean> dispatcherThreadType = new CompletableFuture<>();
                    ConnectionId connectionId = new ConnectionId("test", "thread-test");
                    application.getBean(MessageDispatcherRegistry.class).get(connectionId, "thread-test")
                            .dispatch("key", () -> dispatcherThreadType.complete(Thread.currentThread().isVirtual()));

                    CompletableFuture<Boolean> poolThreadType = new CompletableFuture<>();
                    WebSocketConnectionPool<String, TestPoolConnection> pool = application
                            .getBean(WebSocketConnectionPoolFactory.class)
                            .create(new WebSocketConnectionPoolOptions(1, 1), sequence -> {
                                poolThreadType.complete(Thread.currentThread().isVirtual());
                                return new TestPoolConnection(new ConnectionId("test", "pool-" + sequence));
                            });
                    try {
                        pool.acquire(java.util.List.of("resource"));
                        assertThat(dispatcherThreadType.get(2, TimeUnit.SECONDS)).isEqualTo(virtualThreads);
                        assertThat(poolThreadType.get(2, TimeUnit.SECONDS)).isEqualTo(virtualThreads);
                    } finally {
                        pool.close();
                    }
                });
    }

    private record TestPoolConnection(ConnectionId connectionId) implements PooledWebSocketConnection {
        @Override public void connect() { }
        @Override public void disconnect() { }
        @Override public boolean isConnected() { return false; }
        @Override public void maintain() { }
        @Override public void close() { }
    }
}
