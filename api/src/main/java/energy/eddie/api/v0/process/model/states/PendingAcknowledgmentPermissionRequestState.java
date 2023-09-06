package energy.eddie.api.v0.process.model.states;

import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;

public interface PendingAcknowledgmentPermissionRequestState extends PermissionRequestState {
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
}
