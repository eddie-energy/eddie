package energy.eddie.regionconnector.fr.enedis.permission.request.dtos;

import java.net.URI;

public record CreatedPermissionRequest(String permissionId, URI redirectUri) {
}
