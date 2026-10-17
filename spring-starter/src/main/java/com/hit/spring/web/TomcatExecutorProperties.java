package com.hit.spring.web;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("app.tomcat.executor")
public class TomcatExecutorProperties {

    private String threadNamePrefix = "tomcat.handler-";
    private Integer concurrencyLimit = 1000;

}
