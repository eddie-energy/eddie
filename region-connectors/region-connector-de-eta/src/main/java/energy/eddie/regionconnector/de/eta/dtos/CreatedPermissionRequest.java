package energy.eddie.regionconnector.de.eta.dtos;

/**
 * DTO returned after a permission request is successfully created.
 */
public record CreatedPermissionRequest(String permissionId, String redirectUri) {
}
