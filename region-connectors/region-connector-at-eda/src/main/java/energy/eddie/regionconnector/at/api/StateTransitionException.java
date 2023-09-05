package energy.eddie.regionconnector.at.api;

abstract class StateTransitionException extends Exception {
    protected final Class<? extends PermissionRequestState> permissionRequestStateClass;

    protected StateTransitionException(Class<? extends PermissionRequestState> permissionRequestStateClass) {
        this.permissionRequestStateClass = permissionRequestStateClass;
    }

    protected StateTransitionException(PermissionRequestState permissionRequestState) {
        this(permissionRequestState.getClass());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "permissionRequestStateClass=" + permissionRequestStateClass +
                '}';
    }
}
