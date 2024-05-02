package energy.eddie.regionconnector.at.eda.dto;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public record IdentifiableMasterData(
        EdaMasterData masterData,
        AtPermissionRequest permissionRequest
) {
}
