package com.hit.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("app")
public class ApplicationProperties {

    private String name = "HIT-APP";

    private Boolean enableLogRequestHttp;

    private Boolean enableDebugLogRequestHttp;

}
