package energy.eddie.aiida.errors.permission;

import java.util.UUID;

public class PermissionAlreadyExistsException extends Exception {
    public PermissionAlreadyExistsException(UUID permissionId) {
        super("Permission with ID '%s' already exists.".formatted(permissionId));
    }
}
