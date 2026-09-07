package com.hit.websocket.client.autoconfigure;

import com.hit.websocket.client.connection.WebSocketClientManagerFactory;
import com.hit.websocket.client.dispatch.MessageDispatcherRegistry;
import com.hit.websocket.client.observability.ConnectionSnapshotQuery;
import org.junit.jupiter.api.Test;
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
                    assertThat(application).hasSingleBean(WebSocketClientManagerFactory.class);
                    assertThat(application).hasSingleBean(MessageDispatcherRegistry.class);
                });
    }

    @Test
    void observabilityIsOptIn() {
        context.run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application).doesNotHaveBean(ConnectionSnapshotQuery.class);
            assertThat(application).hasSingleBean(WebSocketClientManagerFactory.class);
        });
    }
}
