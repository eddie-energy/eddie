package energy.eddie.regionconnector.shared.web;

public final class RestApiPaths {
    // These paths should stay hardcoded, it does not make sense to make them configurable
    @SuppressWarnings("java:S1075")
    public static final String PATH_PERMISSION_REQUEST = "/permission-request";
    @SuppressWarnings("java:S1075")
    public static final String PATH_PERMISSION_STATUS_WITH_PATH_PARAM = "/permission-status/{permissionId}";
    public static final String SWAGGER_DOC_PATH = "v3/api-docs";
    @SuppressWarnings("java:S1075")
    public static final String PATH_PERMISSION_ACCEPTED = "/permission-request/{permissionId}/accepted";
    @SuppressWarnings("java:S1075")
    public static final String PATH_PERMISSION_REJECTED = "/permission-request/{permissionId}/rejected";

    private RestApiPaths() {
    }
}
