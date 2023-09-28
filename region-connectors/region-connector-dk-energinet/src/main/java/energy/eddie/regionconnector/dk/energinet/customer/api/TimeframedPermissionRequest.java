package energy.eddie.regionconnector.dk.energinet.customer.api;

import energy.eddie.api.v0.process.model.PermissionRequest;

import java.time.ZonedDateTime;

public interface TimeframedPermissionRequest extends PermissionRequest {
    ZonedDateTime start();

    ZonedDateTime end();
}
