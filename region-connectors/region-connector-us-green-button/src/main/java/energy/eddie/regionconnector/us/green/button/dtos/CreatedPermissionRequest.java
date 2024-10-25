package energy.eddie.regionconnector.us.green.button.dtos;

import java.net.URI;

public record CreatedPermissionRequest(String permissionId, URI redirectUri) {
}
