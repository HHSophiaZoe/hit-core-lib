package com.hit.spring.service.http.impl;

import com.hit.spring.service.http.HttpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Primary
@Component
@ConditionalOnProperty(
        value = {"http-client.default.enable"},
        havingValue = "true"
)
public class DefaultRestTemplateServiceImpl extends RestTemplateServiceBase implements HttpService {

    public DefaultRestTemplateServiceImpl(@Qualifier("defaultRestTemplate") RestTemplate template) {
        super(template);
    }

}