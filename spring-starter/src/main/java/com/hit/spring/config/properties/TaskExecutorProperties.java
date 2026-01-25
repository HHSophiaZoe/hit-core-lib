package com.hit.spring.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties("app.task.executor")
public class TaskExecutorProperties {

    private Boolean enable = Boolean.FALSE;
    private int taskTimeoutSeconds = 30;
    private Pool pool = new Pool();
    private Simple simple = new Simple();

    @Setter
    @Getter
    public static class Pool {
        private String threadNamePrefix = "app.pool-";
        private int coreSize = 10;
        private int maxSize = 50;
        private int queueCapacity = 10000;
        private boolean allowCoreThreadTimeout = true;
        private int keepAliveSeconds = 60;
    }

    @Setter
    @Getter
    public static class Simple {
        private String threadNamePrefix = "app.virtual-";
        private Integer concurrencyLimit = 5000;
    }

}
