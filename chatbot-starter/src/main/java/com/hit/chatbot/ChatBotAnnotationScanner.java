package com.hit.chatbot;

import com.hit.chatbot.annotation.ChatBotMessageListener;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Registers every bean that has a {@link ChatBotMessageListener} method with the {@link ChatBotMessageDispatcher}
 * once the bean is fully initialized (proxy included). Methods are found the same way as the dispatcher registers
 * them.
 */
public class ChatBotAnnotationScanner implements BeanPostProcessor {

    private final ObjectProvider<ChatBotMessageDispatcher> dispatcher;

    public ChatBotAnnotationScanner(ObjectProvider<ChatBotMessageDispatcher> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        // Cheap check first, as Spring does for @EventListener: skips JDK and framework classes
        if (AnnotationUtils.isCandidateClass(targetClass, ChatBotMessageListener.class)
                && !ChatBotMessageDispatcher.findListenerMethods(targetClass).isEmpty()) {
            dispatcher.getObject().addListener(beanName, bean);
        }
        return bean;
    }
}
