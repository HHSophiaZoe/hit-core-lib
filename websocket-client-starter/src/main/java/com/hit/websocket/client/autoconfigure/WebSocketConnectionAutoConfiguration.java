package com.hit.websocket.client.autoconfigure;

import com.hit.websocket.client.connection.WebSocketClientFactory;
import com.hit.websocket.client.observability.WebSocketObserver;
import com.hit.websocket.client.transport.WebSocketTransportFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import java.time.Clock;
import java.util.List;

@AutoConfiguration(after = {WebSocketTransportAutoConfiguration.class, WebSocketObservabilityAutoConfiguration.class})
public class WebSocketConnectionAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    WebSocketClientFactory webSocketClientFactory(
            WebSocketTransportFactory transportFactory, List<WebSocketObserver> observers,
            @Qualifier("webSocketObservabilityClock") Clock clock) {
        return new WebSocketClientFactory(transportFactory, observers, clock);
    }
}
