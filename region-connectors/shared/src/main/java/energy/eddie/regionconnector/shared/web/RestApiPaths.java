package energy.eddie.regionconnector.shared.web;

public final class RestApiPaths {
    private RestApiPaths() {
    }

    // These paths should stay hardcoded, it does not make sense to make them configurable
    @SuppressWarnings("java:S1075")
    public static final String PATH_PERMISSION_REQUEST = "/permission-request";
    @SuppressWarnings("java:S1075")
    public static final String PATH_PERMISSION_STATUS_WITH_PATH_PARAM = "/permission-status/{permissionId}";
}
