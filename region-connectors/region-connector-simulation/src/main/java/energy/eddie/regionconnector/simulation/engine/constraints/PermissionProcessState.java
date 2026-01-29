// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.v0.PermissionProcessStatus;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static energy.eddie.api.v0.PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR;

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
            Set.of(SENT_TO_PERMISSION_ADMINISTRATOR, PermissionProcessStatus.UNABLE_TO_SEND)
    );
    private static final PermissionProcessState UNABLE_TO_SEND = new PermissionProcessState(
            PermissionProcessStatus.UNABLE_TO_SEND,
            Set.of(PermissionProcessStatus.VALIDATED)
    );
    private static final PermissionProcessState SENT_TO_PA = new PermissionProcessState(
            SENT_TO_PERMISSION_ADMINISTRATOR,
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
    private static final Map<PermissionProcessStatus, PermissionProcessState> STATE_MAP;

    static {
        var tmp = new EnumMap<PermissionProcessStatus, PermissionProcessState>(PermissionProcessStatus.class);
        tmp.put(PermissionProcessStatus.CREATED, CREATED);
        tmp.put(PermissionProcessStatus.VALIDATED, VALIDATED);
        tmp.put(PermissionProcessStatus.MALFORMED, MALFORMED);
        tmp.put(PermissionProcessStatus.UNABLE_TO_SEND, UNABLE_TO_SEND);
        tmp.put(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, SENT_TO_PA);
        tmp.put(PermissionProcessStatus.TIMED_OUT, TIMED_OUT);
        tmp.put(PermissionProcessStatus.ACCEPTED, ACCEPTED);
        tmp.put(PermissionProcessStatus.REJECTED, REJECTED);
        tmp.put(PermissionProcessStatus.INVALID, INVALID);
        tmp.put(PermissionProcessStatus.REVOKED, REVOKED);
        tmp.put(PermissionProcessStatus.TERMINATED, TERMINATED);
        tmp.put(PermissionProcessStatus.FULFILLED, FULFILLED);
        tmp.put(PermissionProcessStatus.UNFULFILLABLE, UNFULFILLABLE);
        tmp.put(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION, REQUIRES_EXTERNAL_TERMINATION);
        tmp.put(PermissionProcessStatus.FAILED_TO_TERMINATE, FAILED_TO_TERMINATE);
        tmp.put(PermissionProcessStatus.EXTERNALLY_TERMINATED, EXTERNALLY_TERMINATED);
        STATE_MAP = Collections.unmodifiableMap(tmp);
    }

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

    // All permission statuses are present in the map, so there will be no way to get a null value.
    @SuppressWarnings("NullAway")
    public static PermissionProcessState create(PermissionProcessStatus status) {
        return STATE_MAP.get(status);
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
