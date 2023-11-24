package energy.eddie.api.v0.process.model;

public abstract class StateTransitionException extends Exception {
    protected final Class<? extends PermissionRequestState> permissionRequestStateClass;

    protected StateTransitionException(Class<? extends PermissionRequestState> permissionRequestStateClass) {
        super("Error transitioning: %s".formatted(permissionRequestStateClass));
        this.permissionRequestStateClass = permissionRequestStateClass;
    }

    protected StateTransitionException(PermissionRequestState permissionRequestState) {
        this(permissionRequestState.getClass());
    }

    protected StateTransitionException(PermissionRequestState permissionRequestState, String message) {
        super(message);
        this.permissionRequestStateClass = permissionRequestState.getClass();
    }
}
