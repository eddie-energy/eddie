package energy.eddie.api.agnostic.process.model;

import java.time.ZonedDateTime;

public interface TimeframedPermissionRequest extends PermissionRequest {
    ZonedDateTime start();

    ZonedDateTime end();
}