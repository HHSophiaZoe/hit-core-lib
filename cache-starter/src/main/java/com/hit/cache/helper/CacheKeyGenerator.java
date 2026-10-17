package com.hit.cache.helper;

import com.hit.cache.config.properties.CacheConfigProperties;

/**
 * Builds cache keys of the application: {@code <app-cache><delimiter><key>[<delimiter><param>]...},
 * e.g. {@code trading::lock::order-1}.
 */
public class CacheKeyGenerator {

    private final String prefix;

    private final String delimiter;

    public CacheKeyGenerator(CacheConfigProperties properties) {
        this.prefix = properties.getAppCache();
        this.delimiter = properties.getDelimiter();
    }

    public String build(String key, String... params) {
        StringBuilder sb = new StringBuilder(prefix).append(delimiter).append(key);
        for (String param : params) {
            sb.append(delimiter).append(param);
        }
        return sb.toString();
    }
}
