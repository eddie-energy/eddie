package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Entity
@SuppressWarnings("NullAway") // Needed for JPA
public class CreatedEvent extends PersistablePermissionEvent {
    private final String connectionId;
    private final String dataNeedId;
    @Embedded
    private final EdaDataSourceInformation dataSourceInformation;
    private final ZonedDateTime created;
    private final LocalDate permissionStart;
    private final LocalDate permissionEnd;
    private final String meteringPointId;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;
    private final String cmRequestId;
    private final String conversationId;

    public CreatedEvent() {
        connectionId = null;
        dataNeedId = null;
        dataSourceInformation = null;
        created = null;
        permissionStart = null;
        permissionEnd = null;
        meteringPointId = null;
        granularity = null;
        cmRequestId = null;
        conversationId = null;
    }

    public CreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            EdaDataSourceInformation dataSourceInformation,
            LocalDate start,
            LocalDate end,
            String meteringPointId,
            Granularity granularity,
            String cmRequestId,
            String conversationId
    ) {
        this(permissionId, connectionId, dataNeedId, dataSourceInformation, ZonedDateTime.now(AT_ZONE_ID), start, end,
             meteringPointId, granularity, cmRequestId, conversationId);
    }

    public CreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            EdaDataSourceInformation dataSourceInformation,
            ZonedDateTime created,
            LocalDate start,
            LocalDate end,
            String meteringPointId,
            Granularity granularity,
            String cmRequestId,
            String conversationId
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.dataSourceInformation = dataSourceInformation;
        this.created = created;
        this.permissionStart = start;
        this.permissionEnd = end;
        this.meteringPointId = meteringPointId;
        this.granularity = granularity;
        this.cmRequestId = cmRequestId;
        this.conversationId = conversationId;
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

    public ZonedDateTime created() {
        return created;
    }

    public LocalDate start() {
        return permissionStart;
    }

    public LocalDate end() {
        return permissionEnd;
    }

    public String meteringPointId() {
        return meteringPointId;
    }

    public Granularity granularity() {
        return granularity;
    }

    public String cmRequestId() {
        return cmRequestId;
    }

    public String conversationId() {
        return conversationId;
    }
}
