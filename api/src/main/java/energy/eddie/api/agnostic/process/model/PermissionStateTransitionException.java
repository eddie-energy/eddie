package energy.eddie.api.agnostic.process.model;

import energy.eddie.api.v0.PermissionProcessStatus;

import java.util.List;

public class PermissionStateTransitionException extends Exception {
    /**
     * Indicate that a permission state transition was requested, but cannot be executed because the permission current
     * state is not the required previous state for a successful transition. The permission has to be in a state of
     * {@code allowedCurrentStates} that a transition to {@code desiredState} is permitted.
     *
     * @param permissionId         ID of the permission that should be transitioned.
     * @param desiredState         State to which the permission should be transitioned.
     * @param allowedCurrentStates List of states which allow the permission to be transitioned to
     *                             {@code desiredState}.
     * @param currentStatus        Current state of the permission.
     */
    public PermissionStateTransitionException(
            String permissionId,
            PermissionProcessStatus desiredState,
            List<PermissionProcessStatus> allowedCurrentStates,
            PermissionProcessStatus currentStatus
    ) {
        super("Cannot transition permission '%s' to state '%s', as it is not in a one of the permitted states '%s' but in state '%s'".formatted(
                permissionId, desiredState.toString(), allowedCurrentStates.toString(), currentStatus.toString()
        ));
    }

    /**
     * Convenience method for
     * {@link PermissionStateTransitionException#PermissionStateTransitionException(String, PermissionProcessStatus,
     * List, PermissionProcessStatus)} if there is only one state that allows the transition to {@code desiredState}.
     */
    public PermissionStateTransitionException(
            String permissionId,
            PermissionProcessStatus desiredState,
            PermissionProcessStatus allowedCurrentStates,
            PermissionProcessStatus currentStatus
    ) {
        this(permissionId, desiredState, List.of(allowedCurrentStates), currentStatus);
    }
}
