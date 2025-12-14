package com.hit.spring.util;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentUtils implements EnvironmentAware {

    @Override
    @Autowired
    public void setEnvironment(Environment environment) {
        setEnv(environment);
    }

    @Setter
    private static Environment env;

    public static String get(String key){
        return env.getProperty(key);
    }
}
