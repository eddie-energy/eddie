package energy.eddie.api.v0.process.model.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;

public interface UnableToSendPermissionRequestState extends PermissionRequestState {
    @Override
    default PermissionProcessStatus status() {
        return PermissionProcessStatus.UNABLE_TO_SEND;
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
    default void receivedPermissionAdministratorResponse() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    default void accept() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    default void invalid() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    default void reject() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    default void terminate() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    default void fulfill() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    default void revoke() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    default void timeOut() throws PastStateException {
        throw new PastStateException(this);
    }
}