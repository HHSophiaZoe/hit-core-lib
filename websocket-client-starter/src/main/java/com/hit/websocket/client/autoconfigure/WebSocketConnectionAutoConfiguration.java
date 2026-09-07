package com.hit.websocket.client.autoconfigure;

import com.hit.websocket.client.autoconfigure.properties.WebSocketClientSchedulerProperties;
import com.hit.websocket.client.connection.WebSocketClientManagerFactory;
import com.hit.websocket.client.observability.ConnectionObserver;
import com.hit.websocket.client.transport.WebSocketTransportFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@AutoConfiguration(after = {WebSocketTransportAutoConfiguration.class, WebSocketObservabilityAutoConfiguration.class})
@EnableConfigurationProperties(WebSocketClientSchedulerProperties.class)
public class WebSocketConnectionAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "webSocketClientScheduler")
    ScheduledExecutorService webSocketClientScheduler(WebSocketClientSchedulerProperties properties) {
        if (properties.getPoolSize() < 1) {
            throw new IllegalArgumentException("websocket-client.scheduler.pool-size must be positive");
        }
        AtomicInteger threadNumber = new AtomicInteger();
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(properties.getPoolSize(), runnable -> {
            Thread thread = new Thread(runnable, "websocket-client-scheduler-" + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    WebSocketClientManagerFactory webSocketClientManagerFactory(
            WebSocketTransportFactory transportFactory,
            @Qualifier("webSocketClientScheduler") ScheduledExecutorService scheduler,
            List<ConnectionObserver> observers
    ) {
        return new WebSocketClientManagerFactory(transportFactory, scheduler, observers);
    }
}
