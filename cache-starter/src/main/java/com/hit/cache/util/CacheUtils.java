package com.hit.cache.util;

import com.hit.cache.config.properties.CacheConfigProperties;
import com.hit.cache.helper.CacheContext;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CacheUtils {

    public static String buildCacheKey(String key, String... params) {
        CacheConfigProperties cacheConfigProperties = CacheContext.getCacheConfigProperties();
        StringBuilder sb = new StringBuilder(cacheConfigProperties.getAppCache())
                .append(cacheConfigProperties.getDelimiter())
                .append(key);
        for (String param : params) {
            sb.append(cacheConfigProperties.getDelimiter()).append(param);
        }
        return sb.toString();
    }

}
