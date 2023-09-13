package energy.eddie.aiida.error;

public class InvalidPermissionRevocationException extends RuntimeException {
    public InvalidPermissionRevocationException(String permissionId) {
        super("Permission with id %s cannot be revoked. Only a permission with status ACCEPTED, WAITING_FOR_START or STREAMING_DATA may be revoked.".formatted(permissionId));
    }
}
