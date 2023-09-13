package energy.eddie.aiida.error;


public class PermissionNotFoundException extends RuntimeException {
    public PermissionNotFoundException(String permissionId) {
        super("No permission with id %s found.".formatted(permissionId));
    }
}
