package com.hit.test.config;

import com.hit.jpa.properties.DataSourceDefaultProperties;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@Configuration
@Profile("test")
@RequiredArgsConstructor
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "defaultEntityManagerFactory",
        transactionManagerRef = "defaultTransactionManager",
        basePackages = {"com.hit.test.repository"}
)
@ConditionalOnProperty(value = {"datasource.default.enable"}, havingValue = "true")
public class DataSourceDefaultConfig {

    private final DataSourceDefaultProperties properties;

    @Primary
    @Bean(name = {"defaultDataSource"})
    public HikariDataSource dataSource() {
        return new HikariDataSource(properties);
    }

    @Bean(name = {"defaultPropertiesDataSource"})
    public Map<String, Object> dataProperties() {
        return properties.getProperties();
    }

    @Primary
    @Bean(name = {"defaultEntityManagerFactory"})
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder, DataSource defaultDataSource) {
        log.info("DB config defaultDataSource: {}", properties.getJdbcUrl());
        return builder.dataSource(defaultDataSource)
                .properties(this.dataProperties())
                .packages("com.hit.test.entity")
                .persistenceUnit("defaultEntityManager")
                .build();
    }

    @Primary
    @Bean(name = {"defaultTransactionManager"})
    public PlatformTransactionManager transactionManager(@Qualifier("defaultEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}