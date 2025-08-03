package com.hit.spring.config.embedded;

import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.autoconfigure.web.embedded.TomcatVirtualThreadsWebServerFactoryCustomizer;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class EmbeddedTomcatConfig {

    @Bean
    @ConditionalOnThreading(Threading.VIRTUAL)
    public TomcatVirtualThreadsWebServerFactoryCustomizer tomcatVirtualThreadsWebServerFactoryCustomizer() {
        return new TomcatVirtualThreadsWebServerFactoryCustomizer() {
            @Override
            public void customize(ConfigurableTomcatWebServerFactory factory) {
                factory.addProtocolHandlerCustomizers(protocolHandler -> {
                    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
                    executor.setVirtualThreads(true);
                    executor.setThreadNamePrefix("tomcat-virtual-handler-");
                    protocolHandler.setExecutor(executor);
                });
            }

            @Override
            public int getOrder() {
                return Ordered.LOWEST_PRECEDENCE;
            }
        };
    }

}
