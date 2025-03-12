package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.data.needs.EnergyType;

import java.util.Set;

public class CdsServerBuilder {
    private String baseUri;
    private String name;
    private Set<EnergyType> coverages;
    private String clientId;
    private String clientSecret;
    private String tokenEndpoint;
    private String authorizationEndpoint;
    private String parEndpoint;
    private String clientsEndpoint;
    private String usageSegmentsEndpoint;
    private String customerDataClientId;
    private String customerDataClientSecret;
    private Long id = null;
    private String credentialsEndpoint;

    public CdsServerBuilder setBaseUri(String baseUri) {
        this.baseUri = baseUri;
        return this;
    }

    public CdsServerBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public CdsServerBuilder setCoverages(Set<EnergyType> coverages) {
        this.coverages = coverages;
        return this;
    }

    public CdsServerBuilder setAdminClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public CdsServerBuilder setAdminClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public CdsServerBuilder setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
        return this;
    }

    public CdsServerBuilder setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
        return this;
    }

    public CdsServerBuilder setParEndpoint(String parEndpoint) {
        this.parEndpoint = parEndpoint;
        return this;
    }

    public CdsServerBuilder setUsageSegmentsEndpoint(String usageSegmentsEndpoint) {
        this.usageSegmentsEndpoint = usageSegmentsEndpoint;
        return this;
    }

    public CdsServerBuilder setId(Long id) {
        this.id = id;
        return this;
    }

    public CdsServerBuilder setClientsEndpoint(String clientsEndpoint) {
        this.clientsEndpoint = clientsEndpoint;
        return this;
    }

    public CdsServerBuilder setCredentialsEndpoint(String credentialsEndpoint) {
        this.credentialsEndpoint = credentialsEndpoint;
        return this;
    }
    public CdsServerBuilder setCustomerDataClientId(String customerDataClientId) {
        this.customerDataClientId = customerDataClientId;
        return this;
    }

    public CdsServerBuilder setCustomerDataClientSecret(String clientSecret) {
        this.customerDataClientSecret = clientSecret;
        return this;
    }

    public CdsServer build() {
        return new CdsServer(
                id,
                baseUri,
                name,
                coverages,
                clientId,
                clientSecret,
                new CdsEndpoints(
                        tokenEndpoint,
                        authorizationEndpoint,
                        parEndpoint,
                        clientsEndpoint,
                        credentialsEndpoint,
                        usageSegmentsEndpoint
                ),
                customerDataClientId,
                customerDataClientSecret
        );
    }
}