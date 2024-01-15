package energy.eddie.api.agnostic.process.model;


import energy.eddie.api.v0.DataSourceInformation;

import java.time.ZonedDateTime;

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
     * The dataNeedId identifies the data need that should be met by the permission request.
     *
     * @return dataNeedId
     */
    String dataNeedId();

    /**
     * The state of the permission request.
     *
     * @return the current state of the permission request.
     */
    PermissionRequestState state();

    /**
     * Information about the data source associated with the permission request.
     *
     * @return the DataSourceInformation of the PermissionRequest
     */
    DataSourceInformation dataSourceInformation();

    /**
     * The datetime when the permission request first was created.
     *
     * @return the created datetime
     */
    ZonedDateTime created();

    /**
     * After a state transition was successful the permission requests state will be updated using this method.
     * Usually the old state will update this with the new state.
     *
     * @param state the new state of the PermissionRequest
     */
    void changeState(PermissionRequestState state);

    default void validate() throws StateTransitionException {
        state().validate();
    }

    default void sendToPermissionAdministrator() throws StateTransitionException {
        state().sendToPermissionAdministrator();
    }

    default void receivedPermissionAdministratorResponse() throws StateTransitionException {
        state().receivedPermissionAdministratorResponse();
    }

    default void terminate() throws StateTransitionException {
        state().terminate();
    }

    default void accept() throws StateTransitionException {
        state().accept();
    }

    default void invalid() throws StateTransitionException {
        state().invalid();
    }

    default void reject() throws StateTransitionException {
        state().reject();
    }

    default void revoke() throws StateTransitionException {
        state().revoke();
    }

    default void fulfill() throws StateTransitionException {
        state().fulfill();
    }

    default void timeOut() throws StateTransitionException {
        state().timeOut();
    }
}