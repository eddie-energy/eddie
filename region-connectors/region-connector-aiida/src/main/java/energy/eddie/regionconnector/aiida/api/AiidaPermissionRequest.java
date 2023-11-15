package energy.eddie.regionconnector.aiida.api;

import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import energy.eddie.regionconnector.aiida.states.AiidaCreatedPermissionRequestState;

import java.time.Instant;

public class AiidaPermissionRequest implements PermissionRequest {
    private final String permissionId;
    private final String connectionId;
    private final String dataNeedId;
    private final Instant startTime;
    private final Instant expirationTime;
    private PermissionRequestState state;

    public AiidaPermissionRequest(String permissionId, String connectionId, String dataNeedId, Instant startTime,
                                  Instant expirationTime, AiidaRegionConnectorService service) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.startTime = startTime;
        this.expirationTime = expirationTime;
        this.state = new AiidaCreatedPermissionRequestState(this, service);
    }

    /**
     * Timestamp when data sharing should startTime.
     *
     * @return startTime timestamp
     */
    public Instant startTime() {
        return startTime;
    }

    /**
     * Timestamp until how long data should be shared.
     *
     * @return expirationTime timestamp
     */
    public Instant expirationTime() {
        return expirationTime;
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
    public void changeState(PermissionRequestState state) {
        this.state = state;
    }
}
