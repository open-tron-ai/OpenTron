// Production Java serve backend launcher - delegates to Spring Boot backend application.
package io.opentron.cli;

import org.springframework.boot.SpringApplication;
import org.opentron.backend.OpentronBackendApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Serve command launcher. Starts the Spring Boot backend application which provides
 * the OpenAI-compatible API (/v1/chat/completions, /v1/models, etc.).
 * 
 * Usage: java io.opentron.cli.Serve [--host <host>] [--port <port>]
 * - Default host: 127.0.0.1
 * - Default port: 8000
 * 
 * The Spring Boot backend provides:
 * - /v1/chat/completions (with streaming support)
 * - /v1/models
 * - /v1/chat/stream (WebSocket)
 * - /health (Actuator endpoint)
 * - Full CORS for Tauri desktop clients
 */
public class Serve {
    private static final int DEFAULT_PORT = 8000;
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final String DEFAULT_ENGINE_HOST = "http://127.0.0.1:11434";

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String host = DEFAULT_HOST;
        String engineHost = DEFAULT_ENGINE_HOST;

        // Parse CLI arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                    if (i + 1 < args.length) {
                        port = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--host":
                    if (i + 1 < args.length) {
                        host = args[++i];
                    }
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    return;
                case "--engine-host":
                    if (i + 1 < args.length) {
                        engineHost = args[++i];
                    }
                    break;
                default:
                    // ignore unsupported args for now
                    break;
            }
        }

        // Build Spring Boot arguments
        List<String> springArgs = new ArrayList<>();
        springArgs.add("--server.address=" + host);
        springArgs.add("--server.port=" + port);
        springArgs.add("--engine.host=" + engineHost);

        try {
            System.out.printf("Starting Tron Java serve backend on http://%s:%d%n", host, port);
            System.out.printf("Proxy engine host: %s%n", engineHost);
            System.out.println("Available endpoints:");
            System.out.println("  POST   /v1/chat/completions (with stream parameter)");
            System.out.println("  GET    /v1/models");
            System.out.println("  WS     /v1/chat/stream");
            System.out.println("  GET    /health");
            
            // Launch Spring Boot backend application
            SpringApplication.run(OpentronBackendApplication.class, springArgs.toArray(new String[0]));
        } catch (Exception e) {
            System.err.println("Failed to start serve backend: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java io.opentron.cli.Serve [--host <host>] [--port <port>] [--engine-host <url>]");
        System.out.println();
        System.out.println("Starts the Tron Java serve backend which provides OpenAI-compatible APIs.");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --host <host>    Server bind address (default: 127.0.0.1)");
        System.out.println("  --port <port>    Server port (default: 8000)");
        System.out.println("  --engine-host <url>  Inference engine base URL (default: http://127.0.0.1:11434)");
        System.out.println("  --help            Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java io.opentron.cli.Serve");
        System.out.println("  java io.opentron.cli.Serve --port 9000");
    }
}

