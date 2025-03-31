package com.hit.spring.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties("cloudinary")
public class CloudinaryProperties {

    private Boolean enable;

    private String cloudName;

    private String apiKey;

    private String apiSecret;

}
