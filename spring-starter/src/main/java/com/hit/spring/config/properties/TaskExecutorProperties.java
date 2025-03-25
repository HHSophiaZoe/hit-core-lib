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
    private int coreSize = 10;
    private int maxSize = 30;
    private int queueCapacity = 100;
    private String threadNamePrefix = "core-task";
    private int keepAliveSeconds = 1;

}
