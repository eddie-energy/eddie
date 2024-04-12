package energy.eddie.aiida.errors;

public class PermissionAlreadyExistsException extends Exception {
    public PermissionAlreadyExistsException(String permissionId) {
        super("Permission with ID '%s' already exists.".formatted(permissionId));
    }
}
