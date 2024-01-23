package energy.eddie.api.agnostic.process.model;

public abstract class ContextualizedPermissionRequestState<T extends PermissionRequest> implements PermissionRequestState {
    protected final T permissionRequest;


    protected ContextualizedPermissionRequestState(T permissionRequest) {
        this.permissionRequest = permissionRequest;
    }
}