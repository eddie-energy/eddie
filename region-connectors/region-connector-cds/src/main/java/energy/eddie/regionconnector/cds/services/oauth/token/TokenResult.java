package energy.eddie.regionconnector.cds.services.oauth.token;

public sealed interface TokenResult permits CredentialsWithRefreshToken, CredentialsWithoutRefreshToken, InvalidTokenResult {
}
