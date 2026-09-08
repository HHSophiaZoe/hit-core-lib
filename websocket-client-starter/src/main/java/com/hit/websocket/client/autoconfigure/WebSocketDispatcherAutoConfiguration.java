package com.hit.websocket.client.autoconfigure;

import com.hit.websocket.client.autoconfigure.properties.WebSocketClientDispatcherProperties;
import com.hit.websocket.client.dispatch.MessageDispatcherFactory;
import com.hit.websocket.client.dispatch.MessageDispatcherObserver;
import com.hit.websocket.client.dispatch.MessageDispatcherRegistry;
import com.hit.websocket.client.dispatch.micrometer.MicrometerMessageDispatcherObserver;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ThreadFactory;

@AutoConfiguration(afterName = "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration")
@EnableConfigurationProperties(WebSocketClientDispatcherProperties.class)
public class WebSocketDispatcherAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "websocket-client.observability", name = "enabled", havingValue = "true")
    static class MicrometerConfiguration {
        @Bean
        @ConditionalOnMissingBean(MicrometerMessageDispatcherObserver.class)
        MicrometerMessageDispatcherObserver webSocketMicrometerMessageDispatcherObserver(MeterRegistry meterRegistry) {
            return new MicrometerMessageDispatcherObserver(meterRegistry);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    MessageDispatcherFactory messageDispatcherFactory(List<MessageDispatcherObserver> observers,
            @Value("${spring.threads.virtual.enabled:false}") boolean virtualThreads) {
        ThreadFactory threads = virtualThreads
                ? Thread.ofVirtual().name("websocket-dispatcher-", 0).factory()
                : Thread.ofPlatform().daemon().name("websocket-dispatcher-", 0).factory();
        return new MessageDispatcherFactory(observers, threads);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    MessageDispatcherRegistry messageDispatcherRegistry(
            MessageDispatcherFactory dispatcherFactory,
            WebSocketClientDispatcherProperties properties
    ) {
        return new MessageDispatcherRegistry(dispatcherFactory, properties);
    }
}
