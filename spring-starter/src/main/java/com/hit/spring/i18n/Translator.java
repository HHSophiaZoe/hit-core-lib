package com.hit.spring.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Static access to the application {@link MessageSource} with the locale of the current request, for static code such
 * as {@code ResponseFactory}. Spring hands over the message source through {@link MessageSourceAware} when the bean is
 * created by the auto-configuration.
 */
public class Translator implements MessageSourceAware {

    private static MessageSource messageSource;

    @Override
    public void setMessageSource(MessageSource messageSource) {
        Translator.messageSource = messageSource;
    }

    public static String toLocale(String messageCode) {
        return messageSource.getMessage(messageCode, null, LocaleContextHolder.getLocale());
    }

    public static String toLocale(String messageCode, String[] params) {
        return messageSource.getMessage(messageCode, params, LocaleContextHolder.getLocale());
    }
}
