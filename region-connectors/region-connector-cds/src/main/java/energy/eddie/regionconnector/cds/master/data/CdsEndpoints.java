package energy.eddie.regionconnector.cds.master.data;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

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

    public CdsEndpoints(String tokenEndpoint, String authorizationEndpoint, String pushedAuthorizationRequestEndpoint,
                        String clientsEndpoint
    ) {
        this.tokenEndpoint = tokenEndpoint;
        this.authorizationEndpoint = authorizationEndpoint;
        this.pushedAuthorizationRequestEndpoint = pushedAuthorizationRequestEndpoint;
        this.clientsEndpoint = clientsEndpoint;
    }

    public CdsEndpoints(String baseUri, String pushedAuthorizationRequestEndpoint, String clientsEndpoint) {
        this.pushedAuthorizationRequestEndpoint = pushedAuthorizationRequestEndpoint;
        this.tokenEndpoint = buildOAuthEndpointFor(baseUri, "token");
        this.authorizationEndpoint = buildOAuthEndpointFor(baseUri, "authorize");
        this.clientsEndpoint = clientsEndpoint;
    }

    protected CdsEndpoints() {
        tokenEndpoint = null;
        authorizationEndpoint = null;
        pushedAuthorizationRequestEndpoint = null;
        clientsEndpoint = null;
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

    private static String buildOAuthEndpointFor(String cdsBaseUri, String endpoint) {
        return UriComponentsBuilder.fromUriString(cdsBaseUri)
                                   .pathSegment("oauth", endpoint)
                                   .path("/")
                                   .build()
                                   .toString();
    }
}
