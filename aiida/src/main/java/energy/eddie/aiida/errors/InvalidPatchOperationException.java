package energy.eddie.aiida.errors;


/**
 * Thrown to indicate that the requested PatchOperation is invalid in the context it occurs.
 */
public class InvalidPatchOperationException extends RuntimeException {
    /**
     * Constructs an InvalidPatchOperationException with the default message.
     */
    public InvalidPatchOperationException() {
        super("Invalid PatchOperation, permitted values are: REVOKE_PERMISSION.");
    }
}