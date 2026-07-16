package org.opentron.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataSourceConfig: Profile-based database configuration
 * 
 * Profiles:
 * - prod: PostgreSQL (default)
 * - embedded: H2 file-based database for zero external dependencies
 */
@Configuration
public class DataSourceConfig {

    /**
     * Production profile: Uses PostgreSQL configured via application-prod.properties
     * Auto-configured by Spring Boot via spring.datasource properties.
     * No explicit bean needed - Spring auto-detects and configures.
     */
    @Configuration
    @Profile("prod")
    public static class PostgresConfig {
        public PostgresConfig() {
            Logger log = LoggerFactory.getLogger(PostgresConfig.class);
            log.info("Initializing PostgreSQL DataSource (prod profile)");
        }
    }

    /**
     * Embedded profile: Uses H2 database file-based storage
     * Automatically creates schema and manages connections.
     * Database file stored at ~/.opentron/opentron.mv.db
     */
    @Configuration
    @Profile("embedded")
    public static class H2EmbeddedConfig {
        private static final Logger logger = LoggerFactory.getLogger(H2EmbeddedConfig.class);

        public H2EmbeddedConfig() {
            logger.info("Initializing H2 Embedded DataSource (embedded profile)");
            logger.info("Database file: ~/.opentron/opentron.mv.db");
            logger.info("H2 Console available at: http://localhost:8000/h2-console");
        }
    }
}
