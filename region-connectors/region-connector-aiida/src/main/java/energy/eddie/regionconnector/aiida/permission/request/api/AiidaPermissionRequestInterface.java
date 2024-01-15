package energy.eddie.regionconnector.aiida.permission.request.api;


import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;

public interface AiidaPermissionRequestInterface extends TimeframedPermissionRequest {
    /**
     * Topic on which a permission termination request should be published.
     *
     * @return terminationTopic
     */
    String terminationTopic();
}
