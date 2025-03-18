package energy.eddie.regionconnector.cds.master.data;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.net.URI;

@SuppressWarnings("DataFlowIssue")
@Embeddable
public class CdsEndpoints {
    @Column(name = "token_endpoint", nullable = false)
    private final String tokenEndpoint;
    @Column(name = "authorization_endpoint", nullable = false)
    private final String authorizationEndpoint;
    @Column(name = "pushed_authorization_request_endpoint", nullable = false)
    private final String pushedAuthorizationRequestEndpoint;
    @Column(name = "clients_endpoint", nullable = false)
    private final String clientsEndpoint;
    @Column(name = "credentials_endpoint", nullable = false)
    private final String credentialsEndpoint;
    @Column(name = "usage_point_endpoint", nullable = false)
    private final String usagePointEndpoint;
    @Column(name = "accounts_endpoint", nullable = false)
    private final String accountsEndpoint;
    @Column(name = "service_contracts_endpoint", nullable = false)
    private final String serviceContractsEndpoint;
    @Column(name = "service_points_endpoint", nullable = false)
    private final String servicePointsEndpoint;
    @Column(name = "meter_device_endpoint", nullable = false)
    private final String meterDeviceEndpoint;

    @SuppressWarnings("java:S107")
    public CdsEndpoints(
            String tokenEndpoint,
            String authorizationEndpoint,
            String pushedAuthorizationRequestEndpoint,
            String clientsEndpoint,
            String credentialsEndpoint,
            String usagePointEndpoint,
            String accountsEndpoint,
            String serviceContractsEndpoint,
            String servicePointsEndpoint,
            String meterDeviceEndpoint
    ) {
        this.tokenEndpoint = tokenEndpoint;
        this.authorizationEndpoint = authorizationEndpoint;
        this.pushedAuthorizationRequestEndpoint = pushedAuthorizationRequestEndpoint;
        this.clientsEndpoint = clientsEndpoint;
        this.credentialsEndpoint = credentialsEndpoint;
        this.usagePointEndpoint = usagePointEndpoint;
        this.accountsEndpoint = accountsEndpoint;
        this.serviceContractsEndpoint = serviceContractsEndpoint;
        this.servicePointsEndpoint = servicePointsEndpoint;
        this.meterDeviceEndpoint = meterDeviceEndpoint;
    }

    protected CdsEndpoints() {
        tokenEndpoint = null;
        authorizationEndpoint = null;
        pushedAuthorizationRequestEndpoint = null;
        clientsEndpoint = null;
        credentialsEndpoint = null;
        usagePointEndpoint = null;
        accountsEndpoint = null;
        serviceContractsEndpoint = null;
        servicePointsEndpoint = null;
        meterDeviceEndpoint = null;
    }

    public URI tokenEndpoint() {
        return URI.create(tokenEndpoint);
    }

    public URI authorizationEndpoint() {
        return URI.create(authorizationEndpoint);
    }

    public URI pushedAuthorizationRequestEndpoint() {
        return URI.create(pushedAuthorizationRequestEndpoint);
    }

    public URI clientsEndpoint() {
        return URI.create(clientsEndpoint);
    }

    public URI credentialsEndpoint() {
        return URI.create(credentialsEndpoint);
    }

    public URI usagePointEndpoint() {
        return URI.create(usagePointEndpoint);
    }

    public URI accountsEndpoint() {
        return URI.create(accountsEndpoint);
    }

    public URI serviceContractsEndpoint() {
        return URI.create(serviceContractsEndpoint);
    }

    public URI servicePointsEndpoint() {
        return URI.create(servicePointsEndpoint);
    }

    public URI meterDeviceEndpoint() {
        return URI.create(meterDeviceEndpoint);
    }
}
