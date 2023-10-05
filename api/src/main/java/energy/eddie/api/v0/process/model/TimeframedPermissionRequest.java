package energy.eddie.api.v0.process.model;

import java.time.ZonedDateTime;

public interface TimeframedPermissionRequest extends PermissionRequest {
    ZonedDateTime start();

    ZonedDateTime end();
}
