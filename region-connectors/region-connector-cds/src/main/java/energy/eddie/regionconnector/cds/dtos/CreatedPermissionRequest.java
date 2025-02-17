package energy.eddie.regionconnector.cds.dtos;

import jakarta.annotation.Nullable;

import java.net.URI;

public record CreatedPermissionRequest(String permissionId, @Nullable URI urn) {
}
