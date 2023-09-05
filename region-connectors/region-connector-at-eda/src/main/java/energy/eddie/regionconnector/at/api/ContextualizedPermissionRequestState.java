package energy.eddie.regionconnector.at.api;

public abstract class ContextualizedPermissionRequestState implements PermissionRequestState {
    protected final PermissionRequest permissionRequest;


    protected ContextualizedPermissionRequestState(PermissionRequest permissionRequest) {
        this.permissionRequest = permissionRequest;
    }
}
