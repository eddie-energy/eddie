package energy.eddie.regionconnector.aiida.permission.request.api;


import energy.eddie.api.agnostic.process.model.PermissionRequest;
import jakarta.annotation.Nullable;

public interface AiidaPermissionRequestInterface extends PermissionRequest {
    /**
     * Topic on which a permission termination request should be published.
     *
     * @return terminationTopic
     */
    @Nullable
    String terminationTopic();

    /**
     * MQTT username associated with this permission.
     */
    @Nullable
    String mqttUsername();

    /**
     * A message providing further information about the latest status.
     */
    @Nullable
    String message();
}
