package energy.eddie.api.agnostic.process.model;

import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public interface TimeframedPermissionRequest extends PermissionRequest {
    /**
     * The start date from which data is requested.
     */
    ZonedDateTime start();

    /**
     * The end date from which data is requested.
     */
    @Nullable
    ZonedDateTime end();
}