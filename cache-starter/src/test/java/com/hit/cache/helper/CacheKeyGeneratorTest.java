package com.hit.cache.helper;

import com.hit.cache.config.properties.CacheConfigProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheKeyGeneratorTest {

    @Test
    void joinsAppCacheKeyAndParamsWithTheDelimiter() {
        CacheConfigProperties properties = new CacheConfigProperties();
        properties.setAppCache("trading");
        CacheKeyGenerator generator = new CacheKeyGenerator(properties);

        assertThat(generator.build("lock", "order-1")).isEqualTo("trading::lock::order-1");
        assertThat(generator.build("quote:FPT")).isEqualTo("trading::quote:FPT");
    }
}
