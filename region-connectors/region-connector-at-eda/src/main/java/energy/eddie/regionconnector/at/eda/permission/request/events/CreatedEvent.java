package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import jakarta.annotation.Nullable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Entity
@SuppressWarnings({"NullAway", "unused"}) // Needed for JPA
public class CreatedEvent extends PersistablePermissionEvent {
    private final String connectionId;
    private final String dataNeedId;
    private final ZonedDateTime created;
    @Embedded
    private final EdaDataSourceInformation dataSourceInformation;

    @Nullable
    private final String meteringPointId;

    public CreatedEvent() {
        connectionId = null;
        dataNeedId = null;
        created = null;
        dataSourceInformation = null;
        meteringPointId = null;
    }

    public CreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            EdaDataSourceInformation dataSourceInformation,
            @Nullable String meteringPointId
    ) {
        this(permissionId,
             connectionId,
             dataNeedId,
             ZonedDateTime.now(AT_ZONE_ID),
             dataSourceInformation,
             meteringPointId);
    }

    public CreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            ZonedDateTime created,
            EdaDataSourceInformation dataSourceInformation,
            @Nullable String meteringPointId
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.created = created;
        this.dataSourceInformation = dataSourceInformation;
        this.meteringPointId = meteringPointId;
    }

    public String connectionId() {
        return connectionId;
    }

    public String dataNeedId() {
        return dataNeedId;
    }

    public EdaDataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
    }


    @Nullable
    public String meteringPointId() {
        return meteringPointId;
    }
}
