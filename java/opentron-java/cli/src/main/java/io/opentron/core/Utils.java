package io.opentron.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Utility functions for backend HTTP support.
 */
public class Utils {
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    private static String backendUrlOverride = null;

    public static void setBackendUrl(String url) {
        backendUrlOverride = url;
    }

    public static void resetBackendUrl() {
        backendUrlOverride = null;
    }

    public static String getBackendUrl() {
        if (backendUrlOverride != null && !backendUrlOverride.isBlank()) {
            return backendUrlOverride;
        }
        String url = System.getenv("TRON_BACKEND_URL");
        if (url != null && !url.isBlank()) {
            return url;
        }
        return "http://127.0.0.1:8000";
    }

    private static String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    public static String httpGet(String path) throws IOException, InterruptedException {
        String url = getBackendUrl() + normalizePath(path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }
        throw new IOException("Request failed: " + response.statusCode() + " - " + response.body());
    }

    public static String httpPost(String path, String body) throws IOException, InterruptedException {
        String url = getBackendUrl() + normalizePath(path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }
        throw new IOException("Request failed: " + response.statusCode() + " - " + response.body());
    }

    public static String httpDelete(String path) throws IOException, InterruptedException {
        String url = getBackendUrl() + normalizePath(path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }
        throw new IOException("Request failed: " + response.statusCode() + " - " + response.body());
    }

    /**
     * Utility method to detect if we're running in a Java environment.
     *
     * @return true
     */
    public static boolean isJavaEnvironment() {
        return true;
    }
}
