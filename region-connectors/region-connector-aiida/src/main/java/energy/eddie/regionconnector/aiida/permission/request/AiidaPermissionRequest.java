package energy.eddie.regionconnector.aiida.permission.request;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import energy.eddie.regionconnector.aiida.states.AiidaCreatedPermissionRequestState;

import java.time.Instant;

public class AiidaPermissionRequest implements AiidaPermissionRequestInterface {
    private static final AiidaDataSourceInformation dataSourceInformation = new AiidaDataSourceInformation();
    private final String permissionId;
    private final String connectionId;
    private final String dataNeedId;
    private final String terminationTopic;
    private final Instant startTime;
    private final Instant expirationTime;
    private PermissionRequestState state;

    /**
     * Creates a new AiidaPermissionRequest with the specified parameters.
     *
     * @param permissionId     ID of this permission. AIIDA will use the same ID internally.
     * @param connectionId     connectionId that should be used for this new permission request.
     * @param dataNeedId       dataNeedId that should be used for this new permission request.
     * @param terminationTopic Kafka topic, on which a termination request from the EP should be published.
     * @param startTime        Starting from this UTC timestamp, the permission is valid and data should be shared.
     * @param expirationTime   Until this UTC timestamp, the permission is valid and data sharing should stop.
     * @param service          Reference to the service used for e.g. sending connection status messages.
     */
    public AiidaPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String terminationTopic,
            Instant startTime,
            Instant expirationTime,
            AiidaRegionConnectorService service) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.terminationTopic = terminationTopic;
        this.startTime = startTime;
        this.expirationTime = expirationTime;
        this.state = new AiidaCreatedPermissionRequestState(this, service);
    }

    @Override
    public Instant startTime() {
        return startTime;
    }

    @Override
    public Instant expirationTime() {
        return expirationTime;
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