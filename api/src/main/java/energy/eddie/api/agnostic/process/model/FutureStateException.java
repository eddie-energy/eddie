package energy.eddie.api.agnostic.process.model;

public class FutureStateException extends StateTransitionException {
    public FutureStateException(Class<? extends PermissionRequestState> permissionRequestStateClass) {
        super(permissionRequestStateClass);
    }

    public FutureStateException(PermissionRequestState permissionRequestState) {
        super(permissionRequestState);
    }
}