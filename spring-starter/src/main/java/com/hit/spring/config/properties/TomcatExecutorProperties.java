package com.hit.spring.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties("app.tomcat.executor")
public class TomcatExecutorProperties {

    private String threadNamePrefix = "tomcat.handler-";
    private Integer concurrencyLimit = 1000;

}
