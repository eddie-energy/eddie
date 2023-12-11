package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.v0.RegionalInformation;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;

import java.time.ZonedDateTime;

public record SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId,
                                      ZonedDateTime start, ZonedDateTime end,
                                      PermissionRequestState state) implements TimeframedPermissionRequest {
    public SimplePermissionRequest(String permissionId, String connectionId) {
        this(permissionId, connectionId, null, null, null, null);
    }

    public SimplePermissionRequest(String permissionId, String connectionId, ZonedDateTime start, ZonedDateTime end) {
        this(permissionId, connectionId, null, start, end, null);
    }

    @Override
    public RegionalInformation regionalInformation() {
        return new EnedisRegionalInformation();
    }

    @Override
    public void changeState(PermissionRequestState state) {

    }
}