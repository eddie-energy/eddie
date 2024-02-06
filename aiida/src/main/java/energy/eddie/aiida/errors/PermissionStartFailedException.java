package energy.eddie.aiida.errors;

import energy.eddie.aiida.models.permission.Permission;

public class PermissionStartFailedException extends Exception {
    private final transient Permission permission;

    public PermissionStartFailedException(Permission permission) {
        this.permission = permission;
    }

    public Permission permission() {
        return permission;
    }
}
