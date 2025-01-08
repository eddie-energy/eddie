package energy.eddie.regionconnector.shared.web;

@SuppressWarnings("java:S1075")
// These paths should stay hardcoded, it does not make sense to make them configurable
public final class RestApiPaths {
    /**
     * Common path for permission requests.
     * Usually used for POST requests to create permission requests via the EDDIE button
     */
    public static final String PATH_PERMISSION_REQUEST = "/permission-request";
    /**
     * Path to request the current status of a permission request.
     */
    public static final String PATH_PERMISSION_STATUS_WITH_PATH_PARAM = "/permission-status/{permissionId}";
    /**
     * Path to the swagger docs of a region connector.
     */
    public static final String SWAGGER_DOC_PATH = "v3/api-docs";
    /**
     * Only needed for region connectors, where permission requests either have to be accepted manually or that require two redirect URIs.
     */
    public static final String PATH_PERMISSION_ACCEPTED = "/permission-request/{permissionId}/accepted";
    /**
     * Only needed for region connectors, where permission requests either have to be rejected manually or that require two redirect URIs.
     */
    public static final String PATH_PERMISSION_REJECTED = "/permission-request/{permissionId}/rejected";

    private RestApiPaths() {
    }
}
