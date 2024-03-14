package energy.eddie.api.agnostic.process.model.events;

public interface PermissionEventRepository {
    PermissionEvent saveAndFlush(PermissionEvent permissionEvent);
}
