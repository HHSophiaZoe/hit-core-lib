package com.hit.cache.autoconfigure;

import com.hit.cache.helper.CacheKeyGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class HitCacheAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HitCacheAutoConfiguration.class));

    @Test
    void disabledCachesDoNotCreateHelpersOrConnections() {
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(CacheKeyGenerator.class)
                .doesNotHaveBean("redissonClient").doesNotHaveBean("internalCacheManager"));
        runner.withPropertyValues("cache.internal.enable=false", "cache.external.enable=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(CacheKeyGenerator.class));
    }

    @Test
    void enabledInternalCacheCreatesHelperAndManager() {
        runner.withPropertyValues("cache.internal.enable=true").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(CacheKeyGenerator.class).hasBean("internalCacheManager");
            assertThat(context).doesNotHaveBean("redissonClient");
        });
    }
}
