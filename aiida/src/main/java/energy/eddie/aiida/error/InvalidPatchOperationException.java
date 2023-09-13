package energy.eddie.aiida.error;

public class InvalidPatchOperationException extends RuntimeException {
    public InvalidPatchOperationException() {
        super("Invalid PatchOperation, permitted values are: REVOKE_PERMISSION.");
    }
}