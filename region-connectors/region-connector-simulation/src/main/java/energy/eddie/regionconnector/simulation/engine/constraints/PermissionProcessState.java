package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.v0.PermissionProcessStatus;

import java.util.Set;

class PermissionProcessState {
    private static final PermissionProcessState CREATED = new PermissionProcessState(
            PermissionProcessStatus.CREATED,
            Set.of(PermissionProcessStatus.VALIDATED, PermissionProcessStatus.MALFORMED)
    );
    private static final PermissionProcessState MALFORMED = new PermissionProcessState(
            PermissionProcessStatus.MALFORMED
    );
    private static final PermissionProcessState VALIDATED = new PermissionProcessState(
            PermissionProcessStatus.VALIDATED,
            Set.of(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, PermissionProcessStatus.UNABLE_TO_SEND)
    );
    private static final PermissionProcessState UNABLE_TO_SEND = new PermissionProcessState(
            PermissionProcessStatus.UNABLE_TO_SEND,
            Set.of(PermissionProcessStatus.VALIDATED)
    );
    private static final PermissionProcessState SENT_TO_PA = new PermissionProcessState(
            PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
            Set.of(PermissionProcessStatus.TIMED_OUT,
                   PermissionProcessStatus.INVALID,
                   PermissionProcessStatus.REJECTED,
                   PermissionProcessStatus.ACCEPTED)
    );
    private static final PermissionProcessState TIMED_OUT = new PermissionProcessState(
            PermissionProcessStatus.TIMED_OUT
    );
    private static final PermissionProcessState INVALID = new PermissionProcessState(
            PermissionProcessStatus.INVALID
    );
    private static final PermissionProcessState REJECTED = new PermissionProcessState(
            PermissionProcessStatus.REJECTED
    );
    private static final PermissionProcessState ACCEPTED = new PermissionProcessState(
            PermissionProcessStatus.ACCEPTED,
            Set.of(PermissionProcessStatus.FULFILLED,
                   PermissionProcessStatus.TERMINATED,
                   PermissionProcessStatus.UNFULFILLABLE,
                   PermissionProcessStatus.REVOKED)
    );
    private static final PermissionProcessState FULFILLED = new PermissionProcessState(
            PermissionProcessStatus.FULFILLED,
            true,
            Set.of(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION)
    );
    private static final PermissionProcessState TERMINATED = new PermissionProcessState(
            PermissionProcessStatus.TERMINATED,
            true,
            Set.of(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION)
    );
    private static final PermissionProcessState UNFULFILLABLE = new PermissionProcessState(
            PermissionProcessStatus.UNFULFILLABLE,
            true,
            Set.of(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION)
    );
    private static final PermissionProcessState REVOKED = new PermissionProcessState(
            PermissionProcessStatus.REVOKED
    );
    private static final PermissionProcessState FAILED_TO_TERMINATE = new PermissionProcessState(
            PermissionProcessStatus.FAILED_TO_TERMINATE,
            Set.of(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION)
    );
    private static final PermissionProcessState REQUIRES_EXTERNAL_TERMINATION = new PermissionProcessState(
            PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION,
            Set.of(PermissionProcessStatus.EXTERNALLY_TERMINATED, PermissionProcessStatus.FAILED_TO_TERMINATE)
    );
    private static final PermissionProcessState EXTERNALLY_TERMINATED = new PermissionProcessState(
            PermissionProcessStatus.EXTERNALLY_TERMINATED
    );

    private final PermissionProcessStatus current;
    private final boolean finalState;
    private final Set<PermissionProcessStatus> nextStates;

    private PermissionProcessState(PermissionProcessStatus current) {
        this(current, true, Set.of());
    }

    private PermissionProcessState(
            PermissionProcessStatus current,
            Set<PermissionProcessStatus> nextStates
    ) {
        this(current, false, nextStates);
    }

    private PermissionProcessState(
            PermissionProcessStatus current,
            boolean finalState,
            Set<PermissionProcessStatus> nextStates
    ) {
        if (!finalState && nextStates.isEmpty()) {
            throw new IllegalArgumentException("Next state must not be empty when not a final state");
        }
        this.current = current;
        this.finalState = finalState;
        this.nextStates = nextStates;
    }

    @SuppressWarnings("DuplicatedCode") // False positive
    public static PermissionProcessState create(PermissionProcessStatus status) {
        return switch (status) {
            case CREATED -> CREATED;
            case VALIDATED -> VALIDATED;
            case MALFORMED -> MALFORMED;
            case UNABLE_TO_SEND -> UNABLE_TO_SEND;
            case SENT_TO_PERMISSION_ADMINISTRATOR -> SENT_TO_PA;
            case TIMED_OUT -> TIMED_OUT;
            case ACCEPTED -> ACCEPTED;
            case REJECTED -> REJECTED;
            case INVALID -> INVALID;
            case REVOKED -> REVOKED;
            case TERMINATED -> TERMINATED;
            case FULFILLED -> FULFILLED;
            case UNFULFILLABLE -> UNFULFILLABLE;
            case REQUIRES_EXTERNAL_TERMINATION -> REQUIRES_EXTERNAL_TERMINATION;
            case FAILED_TO_TERMINATE -> FAILED_TO_TERMINATE;
            case EXTERNALLY_TERMINATED -> EXTERNALLY_TERMINATED;
        };
    }


    public PermissionProcessState next(PermissionProcessStatus nextState) {
        if (!nextStates.contains(nextState)) {
            throw new IllegalArgumentException("Next state must be one of " + nextStates);
        }
        return create(nextState);
    }

    public boolean nextIsAllowed(PermissionProcessStatus nextState) {
        return nextStates.contains(nextState);
    }

    public boolean isFinalState() {
        return finalState;
    }

    public PermissionProcessStatus status() {
        return current;
    }
}
