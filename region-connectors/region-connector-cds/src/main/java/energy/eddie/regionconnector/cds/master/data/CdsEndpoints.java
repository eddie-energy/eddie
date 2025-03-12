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

    public CdsEndpoints(
            String tokenEndpoint,
            String authorizationEndpoint,
            String pushedAuthorizationRequestEndpoint,
            String clientsEndpoint,
            String credentialsEndpoint,
            String usagePointEndpoint
    ) {
        this.tokenEndpoint = tokenEndpoint;
        this.authorizationEndpoint = authorizationEndpoint;
        this.pushedAuthorizationRequestEndpoint = pushedAuthorizationRequestEndpoint;
        this.clientsEndpoint = clientsEndpoint;
        this.credentialsEndpoint = credentialsEndpoint;
        this.usagePointEndpoint = usagePointEndpoint;
    }

    protected CdsEndpoints() {
        tokenEndpoint = null;
        authorizationEndpoint = null;
        pushedAuthorizationRequestEndpoint = null;
        clientsEndpoint = null;
        credentialsEndpoint = null;
        usagePointEndpoint = null;
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
}
