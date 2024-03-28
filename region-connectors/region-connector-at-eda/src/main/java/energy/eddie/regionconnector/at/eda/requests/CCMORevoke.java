package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;

public record CCMORevoke(AtPermissionRequest permissionRequest, String eligiblePartyId, String reason) {
}
