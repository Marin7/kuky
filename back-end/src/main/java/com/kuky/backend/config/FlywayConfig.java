package com.kuky.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource, Environment env) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        // Local WIP baseline edits change V1 checksums; repair realigns history without wiping data.
        if (Arrays.asList(env.getActiveProfiles()).contains("local")) {
            flyway.repair();
        }
        flyway.migrate();
        return flyway;
    }
}
