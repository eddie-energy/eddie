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

    void validate() throws StateTransitionException;

    void sendToPermissionAdministrator() throws StateTransitionException;

    void receivedPermissionAdministratorResponse() throws StateTransitionException;

    void accept() throws StateTransitionException;

    void invalid() throws StateTransitionException;

    void reject() throws StateTransitionException;

    void terminate() throws StateTransitionException;

    void revoke() throws StateTransitionException;

    void timeLimit() throws StateTransitionException;

    void timeOut() throws StateTransitionException;
}
