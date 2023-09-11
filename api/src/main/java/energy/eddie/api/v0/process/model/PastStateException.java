package energy.eddie.api.v0.process.model;

public class PastStateException extends StateTransitionException {


    public PastStateException(Class<? extends PermissionRequestState> permissionRequestStateClass) {
        super(permissionRequestStateClass);
    }

    public PastStateException(PermissionRequestState permissionRequestState) {
        super(permissionRequestState);
    }
}
