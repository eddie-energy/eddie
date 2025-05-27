package energy.eddie.regionconnector.cds.master.data;

import jakarta.persistence.*;

import java.net.URI;

@Entity(name = "CdsServer")
@Table(schema = "cds", name = "cds_server")
public class CdsServer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;
    @Column(name = "base_uri", columnDefinition = "text", nullable = false)
    private final String baseUri;
    @Column(name = "admin_client_id", nullable = false)
    private final String adminClientId;
    @Column(name = "admin_client_secret", nullable = false)
    private final String adminClientSecret;

    @SuppressWarnings({"NullAway", "java:S107"})
    public CdsServer(
            String baseUri,
            String adminClientId,
            String adminClientSecret
    ) {
        this(null, baseUri, adminClientId, adminClientSecret);
    }

    @SuppressWarnings("java:S107")
    CdsServer(
            Long id,
            String baseUri,
            String adminClientId,
            String adminClientSecret
    ) {
        this.id = id;
        this.baseUri = baseUri;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
    }

    @SuppressWarnings("NullAway")
    protected CdsServer() {
        id = null;
        baseUri = null;
        adminClientId = null;
        adminClientSecret = null;
    }

    @SuppressWarnings("DataFlowIssue")
    public URI baseUri() {
        return URI.create(baseUri);
    }

    public Long id() {
        return id;
    }

    public String adminClientId() {
        return adminClientId;
    }

    public String adminClientSecret() {
        return adminClientSecret;
    }
}
