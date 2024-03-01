package energy.eddie.regionconnector.shared.permission.requests;


import energy.eddie.api.agnostic.process.model.PermissionRequest;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public abstract class TimestampedPermissionRequest implements PermissionRequest {
    private final ZonedDateTime created;

    protected TimestampedPermissionRequest(ZoneId zone) {
        created = ZonedDateTime.now(zone);
    }

    protected TimestampedPermissionRequest(ZonedDateTime created) {
        this.created = created;
    }

    @Override
    public ZonedDateTime created() {
        return created;
    }
}
