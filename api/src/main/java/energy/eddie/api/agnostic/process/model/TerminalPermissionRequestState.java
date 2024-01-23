package energy.eddie.api.agnostic.process.model;

public interface TerminalPermissionRequestState extends PermissionRequestState {
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
    default void revoke() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    default void fulfill() throws PastStateException {
        throw new PastStateException(this);
    }

    @Override
    default void timeOut() throws PastStateException {
        throw new PastStateException(this);
    }
}