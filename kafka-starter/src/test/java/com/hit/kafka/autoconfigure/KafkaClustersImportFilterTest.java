package com.hit.kafka.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaClustersImportFilterTest {

    private static final String[] CANDIDATES = {
            "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
            KafkaClustersImportFilter.SPRING_BOOT_KAFKA,
            "org.springframework.boot.kafka.autoconfigure.metrics.KafkaMetricsAutoConfiguration"};

    @Test
    void springBootKafkaIsLeftOutOnlyWhenClustersAreSet() {
        assertThat(filter(new MockEnvironment()).match(CANDIDATES, null)).containsExactly(true, true, true);
        assertThat(filter(new MockEnvironment().withProperty("messaging.kafka.clusters.main.bootstrap-servers", "a:9092"))
                .match(CANDIDATES, null)).containsExactly(true, false, true);
    }

    @Test
    void anyKeyOfAClusterCounts() {
        MockEnvironment environment = new MockEnvironment().withProperty("messaging.kafka.clusters.main.consumer.group-id", "g");

        assertThat(KafkaClustersImportFilter.hasClusters(environment)).isTrue();
        assertThat(KafkaClustersImportFilter.hasClusters(new MockEnvironment().withProperty("spring.kafka.bootstrap-servers", "a:9092")))
                .isFalse();
    }

    @Test
    void filterIsRegisteredForSpringBoot() {
        assertThat(SpringFactoriesLoader.loadFactoryNames(AutoConfigurationImportFilter.class, getClass().getClassLoader()))
                .contains(KafkaClustersImportFilter.class.getName());
    }

    private static KafkaClustersImportFilter filter(MockEnvironment environment) {
        KafkaClustersImportFilter filter = new KafkaClustersImportFilter();
        filter.setEnvironment(environment);
        return filter;
    }
}
