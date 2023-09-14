package energy.eddie.aiida.error;

/**
 * Thrown to indicate that no permission with the specified ID is saved in this AIIDA instance.
 */
public class PermissionNotFoundException extends RuntimeException {
    /**
     * Constructs an PermissionNotFoundException with the default message, that includes the permissionId.
     *
     * @param permissionId ID of the permission that couldn't be found.
     */
    public PermissionNotFoundException(String permissionId) {
        super("No permission with id %s found.".formatted(permissionId));
    }
}
