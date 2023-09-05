package energy.eddie.regionconnector.at.api;

public class FutureStateException extends StateTransitionException {
    public FutureStateException(Class<? extends PermissionRequestState> permissionRequestStateClass) {
        super(permissionRequestStateClass);
    }

    public FutureStateException(PermissionRequestState permissionRequestState) {
        super(permissionRequestState);
    }
}
