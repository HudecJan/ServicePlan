package com.berit.lids.springboot.application.serviceplan.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class DatabaseConfig {

    @Bean
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties props = new DataSourceProperties();
        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl != null && databaseUrl.startsWith("postgres://")) {
            // Convert postgres://user:pass@host:port/db to jdbc:postgresql://host:port/db
            databaseUrl = databaseUrl.replace("postgres://", "");
            String userInfo = databaseUrl.substring(0, databaseUrl.indexOf("@"));
            String hostAndDb = databaseUrl.substring(databaseUrl.indexOf("@") + 1);

            String username = userInfo.split(":")[0];
            String password = userInfo.split(":")[1];

            props.setUrl("jdbc:postgresql://" + hostAndDb);
            props.setUsername(username);
            props.setPassword(password);
        } else if (databaseUrl != null) {
            props.setUrl(databaseUrl);
        }

        props.setDriverClassName("org.postgresql.Driver");
        return props;
    }
}
