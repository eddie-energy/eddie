package energy.eddie.regionconnector.de.eta.oauth;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity(name = "DeEtaOAuthToken")
@Table(schema = "de_eta", name = "oauth_token")
public class DeEtaOAuthToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id", nullable = false)
    private String connectionId;

    @Column(name = "access_token", columnDefinition = "text", nullable = false)
    private String accessToken; // encrypted

    @Column(name = "refresh_token", columnDefinition = "text")
    private String refreshToken; // encrypted

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "scopes", columnDefinition = "text")
    private String scopes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public DeEtaOAuthToken() {}

    public DeEtaOAuthToken(String connectionId, String accessToken, String refreshToken, LocalDateTime expiresAt, String scopes) {
        this.connectionId = connectionId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.scopes = scopes;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getConnectionId() { return connectionId; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getScopes() { return scopes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setTokens(String encAccessToken, String encRefreshToken, LocalDateTime expiresAt, String scopes) {
        this.accessToken = encAccessToken;
        this.refreshToken = encRefreshToken;
        this.expiresAt = expiresAt;
        this.scopes = scopes;
        this.updatedAt = LocalDateTime.now();
    }
}
