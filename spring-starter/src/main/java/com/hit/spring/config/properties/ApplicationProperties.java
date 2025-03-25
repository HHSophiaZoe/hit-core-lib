package com.hit.spring.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties("app")
public class ApplicationProperties {

    private String applicationShortName;

    private Boolean enableLogRequestHttp;

}
