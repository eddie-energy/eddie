package energy.eddie.regionconnector.nl.mijn.aansluiting.oauth;

import java.net.URI;

public record OAuthRequestPayload(URI uri, String state, String codeVerifier) {
}
