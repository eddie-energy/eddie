package energy.eddie.regionconnector.cds.client;

import java.net.URI;

public record CustomerDataClientCredentials(String clientId, String clientSecret, URI tokenEndpoint) {
}
