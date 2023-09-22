package energy.eddie.api.v0.process.model.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;

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
}
