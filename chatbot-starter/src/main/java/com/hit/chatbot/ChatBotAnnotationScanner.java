package com.hit.chatbot;

import com.hit.chatbot.annotation.ChatBotMessageListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ChatBotAnnotationScanner implements BeanPostProcessor {

    private final ChatBotMessageDispatcher dispatcher;

    private final List<Class<? extends Annotation>> annotations = List.of(ChatBotMessageListener.class);

    private String originalBeanName;

    public ChatBotAnnotationScanner(@Lazy ChatBotMessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        final AtomicBoolean add = new AtomicBoolean();
        ReflectionUtils.doWithMethods(bean.getClass(), methodCallback -> add.set(true), methodFilter -> {
            Iterator<Class<? extends Annotation>> annotations = this.annotations.iterator();

            Class<? extends Annotation> annotationClass;
            do {
                if (!annotations.hasNext()) {
                    return false;
                }

                annotationClass = annotations.next();
            } while (!methodFilter.isAnnotationPresent(annotationClass));

            return true;
        });
        if (add.get()) {
            this.originalBeanName = beanName;
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (this.originalBeanName != null && originalBeanName.equals(beanName)) {
            this.dispatcher.addListener(beanName, bean);
            this.originalBeanName = null;
        }
        return bean;
    }

}
