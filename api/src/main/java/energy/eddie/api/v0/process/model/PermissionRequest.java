package energy.eddie.api.v0.process.model;


/**
 * A PermissionRequest represents the starting point of requesting the permission for data from an MDA.
 * It can have different states depending on where in the process of a specific Permission Administrator the request currently is.
 * This is the context class of the state pattern.
 *
 * @see <a href="https://refactoring.guru/design-patterns/state">State Pattern</link>
 */
public interface PermissionRequest {

    /**
     * The permissionId of a request.
     * It is used internally of EDDIE to map permission requests or incoming consumption data
     *
     * @return permissionId
     */
    String permissionId();

    /**
     * The connectionId is an id that is given by the eligible party using EDDIE.
     *
     * @return connectionId
     */
    String connectionId();

    /**
     * The state of the permission request.
     *
     * @return the current state of the permission request.
     */
    PermissionRequestState state();

    /**
     * After a state transition was successful the permission requests state will be updated using this method.
     * Usually the old state will update this with the new state.
     *
     * @param state the new state of the PermissionRequest
     */
    void changeState(PermissionRequestState state);

    default void validate() throws FutureStateException, PastStateException {
        state().validate();
    }

    default void sendToPermissionAdministrator() throws FutureStateException, PastStateException {
        state().sendToPermissionAdministrator();
    }

    default void receivedPermissionAdministratorResponse() throws FutureStateException, PastStateException {
        state().receivedPermissionAdministratorResponse();
    }

    default void terminate() throws FutureStateException, PastStateException {
        state().terminate();
    }

    default void accept() throws FutureStateException, PastStateException {
        state().accept();
    }

    default void invalid() throws FutureStateException, PastStateException {
        state().invalid();
    }

    default void rejected() throws FutureStateException, PastStateException {
        state().reject();
    }
}
