package org.opentron.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class StorageConfig {
    
    /**
     * HikariCP Connection Pool for PostgreSQL
     * Uses environment variables for configuration
     */
    @Bean
    @ConditionalOnProperty(name = "postgres.enabled", havingValue = "true", matchIfMissing = false)
    public DataSource postgresDataSource() {
        String url = System.getenv("POSTGRES_URL");
        String user = System.getenv("POSTGRES_USER");
        String password = System.getenv("POSTGRES_PASSWORD");
        
        if (url == null || user == null || password == null) {
            throw new RuntimeException("PostgreSQL connection requires POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD environment variables");
        }
        
        System.out.println("[StorageConfig] Configuring PostgreSQL connection pool");
        System.out.println("[StorageConfig] URL: " + url);
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setAutoCommit(true);
        config.setDriverClassName("org.postgresql.Driver");
        config.setPoolName("OpentronHikariPool");
        
        System.out.println("[StorageConfig] Connection pool initialized: max=" + config.getMaximumPoolSize() + ", min=" + config.getMinimumIdle());
        
        return new HikariDataSource(config);
    }
}
