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
    private String threadNamePrefix = "app.executor-";
    private int taskTimeoutSeconds = 30;
    private Pool pool = new Pool();
    private Simple simple = new Simple();

    @Setter
    @Getter
    public static class Pool {
        private int coreSize = 10;
        private int maxSize = 30;
        private int queueCapacity = 1000;
        private boolean allowCoreThreadTimeout = true;
        private int keepAliveSeconds = 60;
    }

    @Setter
    @Getter
    public static class Simple {
        private Integer concurrencyLimit;
    }

}
