package energy.eddie.api.v0.process.model.states;

import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;

public interface AcceptedPermissionRequestState extends PermissionRequestState {
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
}
