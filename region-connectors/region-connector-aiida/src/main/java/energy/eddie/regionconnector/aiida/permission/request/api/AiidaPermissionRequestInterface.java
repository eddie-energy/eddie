package energy.eddie.regionconnector.aiida.permission.request.api;

import energy.eddie.api.v0.process.model.PermissionRequest;

import java.time.Instant;

public interface AiidaPermissionRequestInterface extends PermissionRequest {
    /**
     * Timestamp when data sharing should startTime.
     *
     * @return startTime timestamp
     */
    Instant startTime();

    /**
     * Timestamp until how long data should be shared.
     *
     * @return expirationTime timestamp
     */
    Instant expirationTime();

    /**
     * Topic on which a permission termination request should be published.
     *
     * @return terminationTopic
     */
    String terminationTopic();
}
