package energy.eddie.regionconnector.aiida.exceptions;

public class PermissionInvalidException extends Exception {
    public PermissionInvalidException(String permissionId, String message) {
        super("Permission with ID '%s' is invalid: %s".formatted(permissionId, message));
    }
}
