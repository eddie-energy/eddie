package energy.eddie.regionconnector.us.green.button.permission.request.dtos;

import java.net.URI;

public record CreatedPermissionRequest(String permissionId, URI redirectUri) {
}
