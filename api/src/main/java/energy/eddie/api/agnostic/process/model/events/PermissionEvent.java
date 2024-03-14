package energy.eddie.api.agnostic.process.model.events;

import energy.eddie.api.v0.PermissionProcessStatus;

import java.time.ZonedDateTime;

public interface PermissionEvent {
    /**
     * The permissionId of a request.
     * It is used internally of EDDIE to map permission requests or incoming consumption data
     *
     * @return permissionId
     */
    String permissionId();

    /**
     * The status of the permission event.
     *
     * @return the current status of the permission event.
     */
    PermissionProcessStatus status();

    ZonedDateTime eventCreated();
}
