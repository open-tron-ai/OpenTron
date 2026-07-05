package org.opentron.backend.storage.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "oauth_tokens")
public class OAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connector_id")
    private String connectorId;

    @Column(name = "provider")
    private String provider;

    @Column(name = "access_token", length = 1024)
    private String accessToken;

    @Column(name = "refresh_token", length = 1024)
    private String refreshToken;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public OAuthToken() {}

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConnectorId() { return connectorId; }
    public void setConnectorId(String connectorId) { this.connectorId = connectorId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
