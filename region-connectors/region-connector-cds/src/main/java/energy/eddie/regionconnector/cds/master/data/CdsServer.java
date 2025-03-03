package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import jakarta.persistence.*;

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
    @Column(name = "admin_client_id", nullable = false)
    private final String adminClientId;
    @Column(name = "admin_client_secret", nullable = false)
    private final String adminClientSecret;
    @Embedded
    private final CdsEndpoints endpoints;
    @Column(name = "customer_data_client_id", nullable = false)
    private final String customerDataClientId;

    @SuppressWarnings({"NullAway", "java:S107"})
    public CdsServer(
            String baseUri,
            String name,
            Set<EnergyType> coverages,
            String adminClientId,
            String adminClientSecret,
            CdsEndpoints endpoints
    ) {
        this(null, baseUri, name, coverages, adminClientId, adminClientSecret, endpoints, null);
    }

    public CdsServer(
            String baseUri,
            String name,
            Set<EnergyType> coverages,
            String adminClientId,
            String adminClientSecret,
            CdsEndpoints endpoints,
            String customerDataClientId
    ) {
        this(null, baseUri, name, coverages, adminClientId, adminClientSecret, endpoints, customerDataClientId);
    }

    @SuppressWarnings("java:S107")
    CdsServer(
            Long id,
            String baseUri,
            String name,
            Set<EnergyType> coverages,
            String adminClientId,
            String adminClientSecret,
            CdsEndpoints endpoints,
            String customerDataClientId
    ) {
        this.id = id;
        this.baseUri = baseUri;
        this.name = name;
        this.coverages = coverages;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
        this.endpoints = endpoints;
        this.customerDataClientId = customerDataClientId;
    }

    @SuppressWarnings("NullAway")
    protected CdsServer() {
        id = null;
        baseUri = null;
        name = null;
        coverages = null;
        adminClientId = null;
        adminClientSecret = null;
        endpoints = null;
        customerDataClientId = null;
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

    public String idAsString() {
        return Objects.toString(id);
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

    public CdsEndpoints endpoints() {
        return endpoints;
    }

    public String customerDataClientId() {
        return customerDataClientId;
    }
}
