package energy.eddie.aiida.error;


/**
 * Thrown to indicate that the specified permission cannot be revoked.
 */
public class InvalidPermissionRevocationException extends RuntimeException {
    /**
     * Constructs an InvalidPermissionRevocationException with the default message.
     */
    public InvalidPermissionRevocationException(String permissionId) {
        super("Permission with id %s cannot be revoked. Only a permission with status ACCEPTED, WAITING_FOR_START or STREAMING_DATA may be revoked.".formatted(permissionId));
    }
}
