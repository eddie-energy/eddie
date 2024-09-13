package energy.eddie.api.agnostic.process.model;


import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * A PermissionRequest represents the starting point of requesting the permission for data from an MDA. It can have
 * different states depending on where in the process of a specific Permission Administrator the request currently is.
 * This is the context class of the state pattern.
 *
 * @see <a href="https://refactoring.guru/design-patterns/state">State Pattern</link>
 */
public interface PermissionRequest {

    /**
     * The permissionId of a request. It is used internally of EDDIE to map permission requests or incoming consumption
     * data
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

    PermissionProcessStatus status();

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
     * The start date from which data is requested. (inclusive)
     */
    LocalDate start();

    /**
     * The end date from which data is requested. (inclusive)
     */
    LocalDate end();
}
