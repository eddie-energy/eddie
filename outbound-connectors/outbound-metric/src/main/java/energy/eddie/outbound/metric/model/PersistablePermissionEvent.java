package energy.eddie.outbound.metric.model;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;

import java.time.ZonedDateTime;

public record PersistablePermissionEvent(String permissionId, PermissionProcessStatus status,
                                         ZonedDateTime eventCreated) implements PermissionEvent {

}
