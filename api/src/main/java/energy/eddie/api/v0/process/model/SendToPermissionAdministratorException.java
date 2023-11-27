package energy.eddie.api.v0.process.model;

public class SendToPermissionAdministratorException extends StateTransitionException {
    private final boolean userFault;

    /**
     * Creates a new exception with a message that describes why the sending to the permission administrator failed.
     * The {@code userFault} parameter indicates whether the sending failed because the user supplied incorrect data
     * (e.g. invalid token) or because of any other reason.
     *
     * @param permissionRequestState State of the permission request when the error occurred.
     * @param message                Description of the message that can be forwarded to the UI.
     * @param userFault              True if the sending failed because of e.g. wrong input from the user.
     */
    public SendToPermissionAdministratorException(PermissionRequestState permissionRequestState, String message, boolean userFault) {
        super(permissionRequestState, message);
        this.userFault = userFault;
    }

    public boolean userFault() {
        return userFault;
    }
}