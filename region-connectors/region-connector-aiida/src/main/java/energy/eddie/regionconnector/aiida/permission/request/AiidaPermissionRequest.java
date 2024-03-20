package energy.eddie.regionconnector.aiida.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import energy.eddie.regionconnector.aiida.states.AiidaCreatedPermissionRequestState;
import energy.eddie.regionconnector.shared.permission.requests.TimestampedPermissionRequest;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AiidaPermissionRequest extends TimestampedPermissionRequest implements AiidaPermissionRequestInterface {
    private static final AiidaDataSourceInformation dataSourceInformation = new AiidaDataSourceInformation();
    private final String permissionId;
    private final String connectionId;
    private final String dataNeedId;
    private final String terminationTopic;
    private final LocalDate startDate;
    private final LocalDate expirationDate;
    private PermissionRequestState state;

    /**
     * Creates a new AiidaPermissionRequest with the specified parameters.
     *
     * @param permissionId     ID of this permission. AIIDA will use the same ID internally.
     * @param connectionId     connectionId that should be used for this new permission request.
     * @param dataNeedId       dataNeedId that should be used for this new permission request.
     * @param terminationTopic Kafka topic, on which a termination request from the EP should be published.
     * @param startDate        Starting from this date, the permission is valid and data should be shared.
     * @param expirationDate   Until this date, the permission is valid and data sharing should stop.
     */
    public AiidaPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String terminationTopic,
            LocalDate startDate,
            LocalDate expirationDate
    ) {
        super(ZoneOffset.UTC);
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.terminationTopic = terminationTopic;
        this.startDate = startDate;
        this.expirationDate = expirationDate;
        this.state = new AiidaCreatedPermissionRequestState(this);
    }

    @Override
    public ZonedDateTime start() {
        return startDate.atStartOfDay(ZoneOffset.UTC);
    }

    @Override
    public ZonedDateTime end() {
        return expirationDate.atStartOfDay(ZoneOffset.UTC);
    }

    @Override
    public String terminationTopic() {
        return terminationTopic;
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
    public PermissionRequestState state() {
        return state;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
    }

    @Override
    public void changeState(PermissionRequestState state) {
        this.state = state;
    }
}
