package energy.eddie.regionconnector.cds.services.oauth.authorization;

public record AcceptedResult(String permissionId, String dataNeedId) implements AuthorizationResult {
}
