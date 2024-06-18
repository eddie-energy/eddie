package energy.eddie.api.v0;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;

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
 * @param additionalInformation this field can be used to provide additional information about the status via a JSON
 *                              object
 */
public record ConnectionStatusMessage(
        String connectionId,
        String permissionId,
        String dataNeedId,
        DataSourceInformation dataSourceInformation,
        ZonedDateTime timestamp,
        PermissionProcessStatus status,
        String message,
        @Nullable JsonNode additionalInformation
) {


    public ConnectionStatusMessage(
            String connectionId,
            String permissionId,
            String dataNeedId,
            DataSourceInformation dataSourceInformation,
            PermissionProcessStatus status
    ) {
        this(connectionId,
             permissionId,
             dataNeedId,
             dataSourceInformation,
             ZonedDateTime.now(ZoneId.systemDefault()),
             status,
             "",
             null);
    }

    public ConnectionStatusMessage(
            String connectionId,
            String permissionId,
            String dataNeedId,
            DataSourceInformation dataSourceInformation,
            PermissionProcessStatus status,
            String message
    ) {
        this(connectionId,
             permissionId,
             dataNeedId,
             dataSourceInformation,
             ZonedDateTime.now(ZoneId.systemDefault()),
             status,
             message,
             null);
    }

    public ConnectionStatusMessage(
            String connectionId,
            String permissionId,
            String dataNeedId,
            DataSourceInformation dataSourceInformation,
            PermissionProcessStatus status,
            String message,
            JsonNode additionalInformation
    ) {
        this(connectionId,
             permissionId,
             dataNeedId,
             dataSourceInformation,
             ZonedDateTime.now(ZoneId.systemDefault()),
             status,
             message,
             additionalInformation);
    }
}
