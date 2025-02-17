package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import jakarta.persistence.*;

import java.net.URI;
import java.util.Objects;
import java.util.Set;

@Entity(name = "CdsServer")
@Table(schema = "cds", name = "cds_server")
public class CdsServer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;
    @Column(name = "base_uri", columnDefinition = "text", nullable = false)
    private final String baseUri;
    @Column(columnDefinition = "text", nullable = false)
    private final String name;
    @ElementCollection
    @CollectionTable(
            name = "coverage",
            joinColumns = @JoinColumn(name = "cds_server_id"),
            schema = "cds"
    )
    @Column(name = "energy_type")
    @Enumerated(EnumType.STRING)
    private final Set<EnergyType> coverages;
    @Column(name = "client_id", nullable = false)
    private final String clientId;
    @Column(name = "client_secret", nullable = false)
    private final String clientSecret;
    @Column(name = "token_endpoint", nullable = false)
    private final String tokenEndpoint;
    @Column(name = "authorization_endpoint", nullable = false)
    private final String authorizationEndpoint;
    @Column(name = "pushed_authorization_request_endpoint", nullable = false)
    private final String pushedAuthorizationRequestEndpoint;

    @SuppressWarnings({"NullAway","java:S107"})
    public CdsServer(
            String baseUri,
            String name,
            Set<EnergyType> coverages,
            String clientId,
            String clientSecret,
            String tokenEndpoint,
            String authorizationEndpoint,
            String parEndpoint
    ) {
        this(null, baseUri, name, coverages, clientId, clientSecret, tokenEndpoint, authorizationEndpoint, parEndpoint);
    }

    @SuppressWarnings("java:S107")
    CdsServer(
            Long id,
            String baseUri,
            String name,
            Set<EnergyType> coverages,
            String clientId,
            String clientSecret,
            String tokenEndpoint,
            String authorizationEndpoint,
            String parEndpoint
    ) {
        this.id = id;
        this.baseUri = baseUri;
        this.name = name;
        this.coverages = coverages;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenEndpoint = tokenEndpoint;
        this.authorizationEndpoint = authorizationEndpoint;
        this.pushedAuthorizationRequestEndpoint = parEndpoint;
    }

    @SuppressWarnings("NullAway")
    protected CdsServer() {
        id = null;
        baseUri = null;
        name = null;
        coverages = null;
        clientId = null;
        clientSecret = null;
        tokenEndpoint = null;
        authorizationEndpoint = null;
        pushedAuthorizationRequestEndpoint = null;
    }

    public String baseUri() {
        return baseUri;
    }

    public String name() {
        return name;
    }

    public String displayName() {
        return "%s - %s".formatted(name, baseUri);
    }

    public Set<EnergyType> coverages() {
        return coverages;
    }

    public String id() {
        return Objects.toString(id);
    }

    public String clientId() {
        return clientId;
    }

    public String clientSecret() {
        return clientSecret;
    }

    @SuppressWarnings("DataFlowIssue")
    public URI tokenEndpoint() {
        return URI.create(tokenEndpoint);
    }

    @SuppressWarnings("DataFlowIssue")
    public URI authorizationEndpoint() {
        return URI.create(authorizationEndpoint);
    }

    @SuppressWarnings("DataFlowIssue")
    public URI pushedAuthorizationRequestEndpoint() {
        return URI.create(pushedAuthorizationRequestEndpoint);
    }
}
