package energy.eddie.regionconnector.cds.services.oauth.code;

import java.net.URI;

public record AuthorizationCodeResult(URI redirectUri, String state) {
}
