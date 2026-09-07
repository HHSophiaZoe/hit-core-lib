package com.hit.websocket.client.autoconfigure;

import com.hit.websocket.client.transport.WebSocketTransportFactory;
import com.hit.websocket.client.transport.reactor.ReactorNettyWebSocketTransportFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import reactor.netty.http.client.HttpClient;

@AutoConfiguration
@ConditionalOnClass(HttpClient.class)
public class WebSocketTransportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(WebSocketTransportFactory.class)
    WebSocketTransportFactory reactorNettyWebSocketTransportFactory() {
        return new ReactorNettyWebSocketTransportFactory(HttpClient.create());
    }
}
