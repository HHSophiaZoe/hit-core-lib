package com.hit.spring.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties("mail")
public class MailProperties {

    private Boolean enable;

    private String host = "smtp.gmail.com";

    private int port = 587;

    private String defaultEncoding = "UTF-8";

    private String username;

    private String password;

}
