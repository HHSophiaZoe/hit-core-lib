package com.hit.spring.util;

import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class BeanUtils implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext context) {
        setContext(context);
    }

    @Setter
    private static ApplicationContext context;

    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext not initialized yet");
        }
        return context.getBean(beanClass);
    }

    public static <T> T getBean(String beanName, Class<T> beanClass) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext not initialized yet");
        }
        return context.getBean(beanName, beanClass);
    }
}
