package energy.eddie.regionconnector.aiida.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;


@Entity
@Table(schema = AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID, name = "aiida_permission_request_view")
public class AiidaPermissionRequest implements AiidaPermissionRequestInterface {
    private static final AiidaDataSourceInformation dataSourceInformation = new AiidaDataSourceInformation();
    @Id
    @Column(name = "permission_id")
    private final String permissionId;
    @Column(name = "connection_id")
    private final String connectionId;
    @Column(name = "data_need_id")
    private final String dataNeedId;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private final PermissionProcessStatus status;
    @Column(name = "termination_topic")
    private final String terminationTopic;
    @Column(name = "mqtt_username")
    private final String mqttUsername;
    @Column(name = "created")
    private final Instant created;

    @SuppressWarnings("NullAway") // Needed for JPA
    protected AiidaPermissionRequest() {
        this.permissionId = null;
        this.connectionId = null;
        this.dataNeedId = null;
        this.start = null;
        this.end = null;
        this.status = null;
        this.terminationTopic = null;
        this.mqttUsername = null;
        this.created = null;
    }

    @SuppressWarnings("java:S107") // permission request requires a lot of top-level fields
    public AiidaPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            LocalDate start,
            LocalDate end,
            PermissionProcessStatus status,
            String terminationTopic,
            String mqttUsername,
            Instant created
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.start = start;
        this.end = end;
        this.status = status;
        this.terminationTopic = terminationTopic;
        this.mqttUsername = mqttUsername;
        this.created = created;
    }

    @Override
    public String permissionId() {
        return permissionId;
    }

    @Override
    public String connectionId() {
        return connectionId;
    }

    @Override
    public String dataNeedId() {
        return dataNeedId;
    }

    @Override
    public LocalDate start() {
        return start;
    }

    @Override
    public LocalDate end() {
        return end;
    }

    @Override
    public PermissionProcessStatus status() {
        return status;
    }

    @Override
    public String terminationTopic() {
        return terminationTopic;
    }

    @Override
    @SuppressWarnings("NullAway") // TODO remove in GH-892
    public PermissionRequestState state() {
        return null;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
    }

    @Override
    @SuppressWarnings("NullAway") // TODO remove in GH-892
    // TODO use Instant instead of ZonedDateTime because with ZonedDateTime we have again a problem that PostgreSQL doesn't include the TZ information when we read the dates, so just use UTC timestamps
    public ZonedDateTime created() {
        return created.atZone(AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID);
    }

    @Override
    public void changeState(PermissionRequestState state) {
        // NoOp
    }
}
