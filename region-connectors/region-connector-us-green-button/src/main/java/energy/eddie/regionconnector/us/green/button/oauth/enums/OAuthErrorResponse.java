package energy.eddie.regionconnector.us.green.button.oauth.enums;

public enum OAuthErrorResponse {
    INVALID_REQUEST("invalid_request"),                     //"The request is missing a required parameter, includes an invalid parameter value, includes a parameter more than once, or is otherwise malformed.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_CLIENT("unauthorized_client"),             //"The client is not authorized to request an authorization code using this method.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("access_denied"),                         //"The resource owner or authorization server denied the request.", HttpStatus.FORBIDDEN),
    UNSUPPORTED_RESPONSE_TYPE("unsupported_response_type"), //"The authorization server does not support obtaining an authorization code using this method.", HttpStatus.BAD_REQUEST),
    INVALID_SCOPE("invalid_scope"),                         //"The requested scope is invalid, unknown, or malformed.", HttpStatus.BAD_REQUEST),
    SERVER_ERROR("server_error"),                           //"The authorization server encountered an unexpected condition that prevented it from fulfilling the request.", HttpStatus.INTERNAL_SERVER_ERROR),
    TEMPORARILY_UNAVAILABLE("temporarily_unavailable"),     //"The authorization server is currently unable to handle the request due to a temporary overloading or maintenance of the server.", HttpStatus.SERVICE_UNAVAILABLE),
    UNKNOWN_ERROR("unknown_error");                         //"An unknown error occurred", HttpStatus.INTERNAL_SERVER_ERROR

    private final String error;

    OAuthErrorResponse(String error) {
        this.error = error;
    }

    public static OAuthErrorResponse fromError(String error) {
        for (OAuthErrorResponse oAuthErrorResponse : values()) {
            if (oAuthErrorResponse.error.equals(error)) {
                return oAuthErrorResponse;
            }
        }
        return UNKNOWN_ERROR;
    }

    public String error() {
        return error;
    }

    @Override
    public String toString() {
        return error;
    }
}
