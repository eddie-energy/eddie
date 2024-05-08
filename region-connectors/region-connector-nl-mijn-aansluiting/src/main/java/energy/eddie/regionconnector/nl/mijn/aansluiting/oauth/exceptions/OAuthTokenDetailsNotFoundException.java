package energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions;

public class OAuthTokenDetailsNotFoundException extends OAuthException {
    public OAuthTokenDetailsNotFoundException(String permissionId) {
        super("OAuthTokenDetails for permission ID %s not found".formatted(permissionId));
    }
}
