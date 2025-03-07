package energy.eddie.regionconnector.cds.services.oauth.authorization;

public record ErrorResult(String permissionId, String error) implements AuthorizationResult {
}
