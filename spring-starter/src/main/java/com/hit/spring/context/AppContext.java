package com.hit.spring.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hit.spring.config.properties.ApplicationProperties;
import com.hit.spring.config.properties.SecurityProperties;
import com.hit.spring.core.converter.DataConverter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AppContext {

    @Setter
    @Getter
    private static ObjectMapper objectMapper;

    @Setter
    @Getter
    private static DataConverter dataConverter;

    @Setter
    @Getter
    private static ApplicationProperties appProperties;

    @Setter
    @Getter
    private static SecurityProperties securityProperties;

    @Autowired
    AppContext(ObjectMapper objectMapper, @Qualifier("dataConverter") DataConverter dataConverter,
               ApplicationProperties appProperties, SecurityProperties securityProperties) {
        setObjectMapper(objectMapper);
        setDataConverter(dataConverter);
        setAppProperties(appProperties);
        setSecurityProperties(securityProperties);
    }

}
