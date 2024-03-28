package energy.eddie.regionconnector.aiida.permission.request.api;


import energy.eddie.api.agnostic.process.model.PermissionRequest;

public interface AiidaPermissionRequestInterface extends PermissionRequest {
    /**
     * Topic on which a permission termination request should be published.
     *
     * @return terminationTopic
     */
    String terminationTopic();
}
