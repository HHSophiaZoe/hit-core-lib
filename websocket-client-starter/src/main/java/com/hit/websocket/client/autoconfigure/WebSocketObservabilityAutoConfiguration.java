package com.hit.websocket.client.autoconfigure;

import com.hit.websocket.client.autoconfigure.condition.ConditionalOnWebSocketObservabilityEnabled;
import com.hit.websocket.client.autoconfigure.properties.WebSocketClientObservabilityProperties;
import com.hit.websocket.client.observability.ConnectionSnapshotQuery;
import com.hit.websocket.client.observability.memory.InMemoryConnectionStateRegistry;
import com.hit.websocket.client.observability.micrometer.MicrometerConnectionObserver;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@AutoConfiguration(afterName = "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration")
@EnableConfigurationProperties(WebSocketClientObservabilityProperties.class)
public class WebSocketObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnWebSocketObservabilityEnabled
    @ConditionalOnMissingBean(ConnectionSnapshotQuery.class)
    InMemoryConnectionStateRegistry webSocketConnectionStateRegistry(
            @Qualifier("webSocketObservabilityClock") Clock clock,
            WebSocketClientObservabilityProperties properties
    ) {
        return new InMemoryConnectionStateRegistry(clock, properties.getRecentEventCapacity());
    }

    @Bean("webSocketObservabilityClock")
    @ConditionalOnMissingBean(name = "webSocketObservabilityClock")
    Clock webSocketObservabilityClock(WebSocketClientObservabilityProperties properties) {
        return Clock.system(properties.getStatisticsZone());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnWebSocketObservabilityEnabled
    static class MicrometerConfiguration {
        @Bean
        @ConditionalOnMissingBean(MicrometerConnectionObserver.class)
        MicrometerConnectionObserver webSocketMicrometerConnectionObserver(
                MeterRegistry meterRegistry,
                @Qualifier("webSocketObservabilityClock") Clock clock
        ) {
            return new MicrometerConnectionObserver(meterRegistry, clock);
        }
    }
}
