package energy.eddie.regionconnector.cds.services.oauth.par;

import java.net.URI;
import java.time.ZonedDateTime;

public record SuccessfulParResponse(URI redirectUri, ZonedDateTime expiresAt, String state) implements ParResponse {
}
