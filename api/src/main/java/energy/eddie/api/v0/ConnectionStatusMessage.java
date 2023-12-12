package energy.eddie.api.v0;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Represents a generic status message for a permission
 *
 * @param connectionId          id of the connection (a connectionId can be associated with multiple permissions)
 * @param permissionId          unique id of the permission
 * @param dataNeedId            id of the data need associated with the permission
 * @param dataSourceInformation information about the datasource
 * @param timestamp             timestamp of the message
 * @param status                status of the message
 * @param message               contains additional information about the status
 */
public record ConnectionStatusMessage(String connectionId,
                                      String permissionId,
                                      String dataNeedId,
                                      DataSourceInformation dataSourceInformation,
                                      ZonedDateTime timestamp,
                                      PermissionProcessStatus status,
                                      String message) {


    public ConnectionStatusMessage(String connectionId, String permissionId, String dataNeedId, DataSourceInformation dataSourceInformation, PermissionProcessStatus status) {
        this(connectionId, permissionId, dataNeedId, dataSourceInformation, ZonedDateTime.now(ZoneId.systemDefault()), status, "");
    }

    public ConnectionStatusMessage(String connectionId, String permissionId, String dataNeedId, DataSourceInformation dataSourceInformation, PermissionProcessStatus status, String message) {
        this(connectionId, permissionId, dataNeedId, dataSourceInformation, ZonedDateTime.now(ZoneId.systemDefault()), status, message);
    }
}