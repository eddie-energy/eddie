package energy.eddie.api.v0.process.model.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;

public interface CreatedPermissionRequestState extends PermissionRequestState {

    @Override
    default PermissionProcessStatus status() {
        return PermissionProcessStatus.CREATED;
    }

    @Override
    default void sendToPermissionAdministrator() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    default void receivedPermissionAdministratorResponse() throws FutureStateException {
        throw new FutureStateException(this);
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
    default void timeLimit() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    default void timeOut() throws FutureStateException {
        throw new FutureStateException(this);
    }
}
