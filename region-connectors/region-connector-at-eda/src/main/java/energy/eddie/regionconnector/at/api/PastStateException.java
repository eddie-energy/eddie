package energy.eddie.regionconnector.at.api;

public class PastStateException extends StateTransitionException {


    public PastStateException(Class<? extends PermissionRequestState> permissionRequestStateClass) {
        super(permissionRequestStateClass);
    }

    public PastStateException(PermissionRequestState permissionRequestState) {
        super(permissionRequestState);
    }
}
