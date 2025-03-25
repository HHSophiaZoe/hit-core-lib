package com.hit.jpa.config.properties;

import com.zaxxer.hikari.HikariConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@Configuration
@ConfigurationProperties("datasource.default")
public class DataSourceDefaultProperties extends HikariConfig {

    private Boolean enable;

    private Hibernate hibernate;

    private Map<String, Object> properties;

    public Map<String, Object> getProperties() {
        Map<String, Object> result = new HashMap<>();
        result.put(Hibernate.HBM2DDL_AUTO_CONFIG, hibernate.getDdlAuto().getValue());
        result.put(Hibernate.SHOW_SQL_CONFIG, hibernate.getShowSql());
        result.put(Hibernate.FORMAT_SQL_CONFIG, hibernate.getFormatSql());
        result.put(Hibernate.USE_SQL_COMMENTS_CONFIG, hibernate.getUseSqlComments());
        result.put(Hibernate.IMPLICIT_NAMING_STRATEGY_CONFIG, hibernate.getImplicitNamingStrategy());
        result.put(Hibernate.PHYSICAL_NAMING_STRATEGY_CONFIG, hibernate.getPhysicalNamingStrategy());

        if (this.properties != null && !this.properties.isEmpty()) {
            result.putAll(this.properties);
        }
        return result;
    }

    @Setter
    @Getter
    public static class Hibernate {

        public static final String HBM2DDL_AUTO_CONFIG = "hibernate.hbm2ddl.auto";
        public static final String SHOW_SQL_CONFIG = "hibernate.show_sql";
        public static final String FORMAT_SQL_CONFIG = "hibernate.format_sql";
        public static final String USE_SQL_COMMENTS_CONFIG = "hibernate.use_sql_comments";
        public static final String IMPLICIT_NAMING_STRATEGY_CONFIG = "hibernate.implicit_naming_strategy";
        public static final String PHYSICAL_NAMING_STRATEGY_CONFIG = "hibernate.physical_naming_strategy";

        private DdlAuto ddlAuto = DdlAuto.NONE;

        private Boolean showSql = false;

        private Boolean formatSql = false;

        private Boolean useSqlComments = false;

        private String implicitNamingStrategy = SpringImplicitNamingStrategy.class.getName();

        private String physicalNamingStrategy = CamelCaseToUnderscoresNamingStrategy.class.getName();

        @Getter
        @AllArgsConstructor
        public enum DdlAuto {
            CREATE("create"),
            CREATE_DROP("create-drop"),
            NONE("none"),
            UPDATE("update"),
            VALIDATE("validate");

            private final String value;
        }
    }
}
