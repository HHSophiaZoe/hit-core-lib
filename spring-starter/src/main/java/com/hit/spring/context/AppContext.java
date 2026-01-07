package com.hit.spring.context;

import com.hit.spring.config.properties.ApplicationProperties;
import com.hit.spring.config.properties.SecurityProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppContext {

    @Setter
    @Getter
    private static ApplicationProperties appProperties;

    @Setter
    @Getter
    private static SecurityProperties securityProperties;

    @Autowired
    AppContext(ApplicationProperties appProperties, SecurityProperties securityProperties) {
        setAppProperties(appProperties);
        setSecurityProperties(securityProperties);
    }

}
