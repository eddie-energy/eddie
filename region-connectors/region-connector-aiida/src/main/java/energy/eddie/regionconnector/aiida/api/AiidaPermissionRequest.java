package energy.eddie.regionconnector.aiida.api;

import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;

import java.time.Instant;

public class AiidaPermissionRequest implements PermissionRequest {
    private final String permissionId;
    private final String connectionId;
    private final String dataNeedId;
    private final Instant startTime;
    private final Instant expirationTime;

    public AiidaPermissionRequest(String permissionId, String connectionId, String dataNeedId, Instant startTime,
                                  Instant expirationTime) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.startTime = startTime;
        this.expirationTime = expirationTime;
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
    @SuppressWarnings("NullAway")
    public PermissionRequestState state() {
        return null;
    }

    @Override
    public void changeState(PermissionRequestState state) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
