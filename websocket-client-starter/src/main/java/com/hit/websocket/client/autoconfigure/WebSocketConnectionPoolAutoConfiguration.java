package com.hit.websocket.client.autoconfigure;

import com.hit.websocket.client.pool.WebSocketConnectionPoolFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ThreadFactory;

@AutoConfiguration
public class WebSocketConnectionPoolAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    WebSocketConnectionPoolFactory webSocketConnectionPoolFactory(
            @Value("${spring.threads.virtual.enabled:false}") boolean virtualThreads) {
        ThreadFactory threads = virtualThreads
                ? Thread.ofVirtual().name("websocket-pool-", 0).factory()
                : Thread.ofPlatform().daemon().name("websocket-pool-", 0).factory();
        return new WebSocketConnectionPoolFactory(threads);
    }
}
