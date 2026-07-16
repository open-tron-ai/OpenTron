package org.opentron.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * StartupInitializer: Handles initialization on application startup
 * - Creates .opentron data directory for embedded H2 database
 * - Logs active profiles and database configuration
 */
@Configuration
public class StartupInitializer {

    private static final Logger logger = LoggerFactory.getLogger(StartupInitializer.class);

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed(ContextRefreshedEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String[] profiles = env.getActiveProfiles();
        
        // Default to prod if no profile is set
        if (profiles.length == 0) {
            profiles = new String[]{"prod"};
        }

        logger.info("========================================");
        logger.info("OpenTron Backend Startup");
        logger.info("========================================");
        logger.info("Active profiles: {}", Arrays.toString(profiles));
        logger.info("Virtual Threads enabled: {}", env.getProperty("spring.threads.virtual.enabled"));

        // Handle embedded profile initialization
        if (Arrays.asList(profiles).contains("embedded")) {
            initializeEmbeddedMode();
        } else {
            logger.info("Using PostgreSQL (prod/default profile)");
            logger.info("PostgreSQL URL: {}", env.getProperty("spring.datasource.url"));
        }

        logger.info("Server running on port: {}", env.getProperty("server.port"));
        logger.info("Engine host: {}", env.getProperty("engine.host"));
        logger.info("========================================");
    }

    /**
     * Initialize embedded mode: create data directory if needed
     */
    private void initializeEmbeddedMode() {
        try {
            String userHome = System.getProperty("user.home");
            Path dataDir = Paths.get(userHome, ".opentron");
            
            // Create directory if it doesn't exist
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                logger.info("Created .opentron data directory: {}", dataDir);
            } else {
                logger.info("Using existing .opentron data directory: {}", dataDir);
            }

            // Check for existing database file
            Path dbFile = Paths.get(userHome, ".opentron", "opentron.mv.db");
            if (Files.exists(dbFile)) {
                long sizeBytes = Files.size(dbFile);
                logger.info("Existing H2 database found: {} bytes", sizeBytes);
            } else {
                logger.info("New H2 database will be created on first startup");
            }

            logger.info("Embedded mode initialized successfully");
            logger.info("H2 Console available at: http://localhost:8000/h2-console");
        } catch (Exception e) {
            logger.error("Failed to initialize embedded mode", e);
            throw new RuntimeException("Failed to initialize embedded database", e);
        }
    }
}
