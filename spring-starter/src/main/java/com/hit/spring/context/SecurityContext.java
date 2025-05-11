package com.hit.spring.context;

import com.hit.spring.core.data.model.SimpleSecurityUser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityContext {

    private static final ThreadLocal<Map<String, Object>> CONTEXT_MANAGE = new ThreadLocal<>();

    private static final String AUTHENTICATION = "AUTHENTICATION";
    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";

    public static void setSimpleSecurityUser(SimpleSecurityUser simpleSecurityUser) {
        set(AUTHENTICATION, simpleSecurityUser);
    }

    public static SimpleSecurityUser getSimpleSecurityUser() {
        if (CONTEXT_MANAGE.get() != null &&
                CONTEXT_MANAGE.get().containsKey(AUTHENTICATION)) {
            return (SimpleSecurityUser) CONTEXT_MANAGE.get().get(AUTHENTICATION);
        }
        return null;
    }

    public static void setAccessToken(String accessToken) {
        set(ACCESS_TOKEN, accessToken);
    }

    public static String getAccessToken() {
        if (CONTEXT_MANAGE.get() != null &&
                CONTEXT_MANAGE.get().containsKey(ACCESS_TOKEN)) {
            return (String) CONTEXT_MANAGE.get().get(AUTHENTICATION);
        }
        return null;
    }

    private static void set(String key, Object value) {
        if (CONTEXT_MANAGE.get() != null) {
            Map<String, Object> dataMap = CONTEXT_MANAGE.get();
            dataMap.put(key, value);
        } else {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put(key, value);
            CONTEXT_MANAGE.set(dataMap);
        }
    }

    public static void clearContext() {
        CONTEXT_MANAGE.remove();
    }

}