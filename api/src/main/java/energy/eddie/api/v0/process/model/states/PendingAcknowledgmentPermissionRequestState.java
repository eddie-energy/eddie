package energy.eddie.api.v0.process.model.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;

public interface PendingAcknowledgmentPermissionRequestState extends PermissionRequestState {
    @Override
    default PermissionProcessStatus status() {
        return PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT;
    }

    @Override
    default void validate() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    default void sendToPermissionAdministrator() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    default void accept() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    default void invalid() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    default void reject() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    default void terminate() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    default void revoke() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    default void fulfill() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    default void timeOut() throws FutureStateException {
        throw new FutureStateException(this);
    }
}