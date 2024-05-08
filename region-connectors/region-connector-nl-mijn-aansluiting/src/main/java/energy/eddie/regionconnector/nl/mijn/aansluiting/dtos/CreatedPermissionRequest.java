package energy.eddie.regionconnector.nl.mijn.aansluiting.dtos;

import java.net.URI;

public record CreatedPermissionRequest(String permissionId, URI redirectUri) {
}
