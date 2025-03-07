package energy.eddie.regionconnector.cds.services.oauth.authorization;

public sealed interface AuthorizationResult permits AcceptedResult, ErrorResult, UnauthorizedResult {
}
