package energy.eddie.api.v0.process.model;

import energy.eddie.api.v0.PermissionProcessStatus;

/**
 * This is the state of a permission request.
 * Depending on where a request is at the Permission Administrators process the state will be updated accordingly.
 *
 * @see <a href="https://refactoring.guru/design-patterns/state">State Pattern</a>
 */
public interface PermissionRequestState {
    /**
     * The status of the permission request.
     *
     * @return the current status of the permission request.
     */
    PermissionProcessStatus status();

    void validate() throws PastStateException, FutureStateException;

    void sendToPermissionAdministrator() throws PastStateException, FutureStateException;

    void receivedPermissionAdministratorResponse() throws PastStateException, FutureStateException;

    void accept() throws PastStateException, FutureStateException;

    void invalid() throws PastStateException, FutureStateException;

    void reject() throws PastStateException, FutureStateException;

    void terminate() throws PastStateException, FutureStateException;
}
