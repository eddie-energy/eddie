package energy.eddie.regionconnector.cds.services.oauth.authorization;

public record UnauthorizedResult(String permissionId, energy.eddie.api.v0.PermissionProcessStatus status) implements AuthorizationResult {
}
