package com.hit.jpa.autoconfigure;

import com.hit.jpa.TransactionSupport;
import com.hit.jpa.properties.DataSourceDefaultProperties;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Binds {@code datasource.default.*} for the application's own DataSource/EntityManager config and provides
 * {@link TransactionSupport}.
 */
@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
@EnableConfigurationProperties(DataSourceDefaultProperties.class)
public class HitJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TransactionSupport transactionSupport() {
        return new TransactionSupport();
    }
}
