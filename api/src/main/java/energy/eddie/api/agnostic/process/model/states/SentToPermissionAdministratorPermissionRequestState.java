package energy.eddie.api.agnostic.process.model.states;

import energy.eddie.api.agnostic.process.model.FutureStateException;
import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;

public interface SentToPermissionAdministratorPermissionRequestState extends PermissionRequestState {
    @Override
    default PermissionProcessStatus status() {
        return PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR;
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
    default void terminate() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    default void fulfill() throws FutureStateException {
        throw new FutureStateException(this);
    }

    @Override
    default void revoke() throws FutureStateException {
        throw new FutureStateException(this);
    }
}