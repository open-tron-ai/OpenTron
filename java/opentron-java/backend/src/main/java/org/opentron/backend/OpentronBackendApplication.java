package org.opentron.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import org.opentron.backend.util.EngineRouting;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.opentron.backend.websocket.ReactiveChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

@SpringBootApplication
@EnableAsync
public class OpentronBackendApplication {

    private static final Logger logger = LoggerFactory.getLogger(OpentronBackendApplication.class);

    public static void main(String[] args) {
        try {
            SpringApplication.run(OpentronBackendApplication.class, args);
        } catch (Throwable e) {
            logger.error("Startup failed", e);
            System.exit(1);
        }
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/v1/**")
                        .allowedOrigins(
                            "http://localhost:5173",
                            "http://127.0.0.1:5173",
                            "http://localhost:5174",
                            "http://127.0.0.1:5174",
                            "tauri://localhost",
                            "http://tauri.localhost",
                            "https://tauri.localhost"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    public WebClient webClient(@Value("${engine.host:http://localhost:11434}") String engineHost,
                               @Value("${engine.apiKey:}") String apiKey) {
        logger.info("engine.host={} apiKey={}", engineHost, (apiKey == null ? "<null>" : apiKey.isBlank() ? "<blank>" : "<set>"));

        reactor.netty.resources.ConnectionProvider provider = reactor.netty.resources.ConnectionProvider.builder("opentron-pool")
            .maxConnections(200)
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .build();

        HttpClient httpClient = HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);

        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        WebClient.Builder b = WebClient.builder()
                .clientConnector(connector)
                .baseUrl(engineHost);

        if (apiKey != null && !apiKey.isBlank()) {
            b.defaultHeader("Authorization", "Bearer " + apiKey);
        }

        return b.build();
    }

    @Bean
    public WebSocketHandler chatWebSocketHandler(WebClient webClient, EngineRouting engineRouting) {
        return new ReactiveChatWebSocketHandler(webClient, engineRouting);
    }
}
