package energy.eddie.regionconnector.shared.event.sourcing;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;

@FunctionalInterface
public interface PermissionEventFactory {
    PermissionEvent create(String permissionId, PermissionProcessStatus status);
}
