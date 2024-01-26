package energy.eddie.regionconnector.es.datadis.dtos;

public abstract sealed class AuthorizationRequestResponse {

    protected final String originalResponse;

    protected AuthorizationRequestResponse(String response) {
        this.originalResponse = response;
    }

    public static AuthorizationRequestResponse fromResponse(String response) {
        return switch (response) {
            case "ok" -> new Ok(response);
            case "nonif" -> new NoNif(response);
            case "nopermisos" -> new NoPermission(response);
            default -> new Unknown(response);
        };
    }

    public String originalResponse() {
        return originalResponse;
    }


    public static final class Ok extends AuthorizationRequestResponse {
        private Ok(String response) {
            super(response);
        }
    }

    public static final class NoNif extends AuthorizationRequestResponse {
        private NoNif(String response) {
            super(response);
        }
    }

    public static final class NoPermission extends AuthorizationRequestResponse {
        private NoPermission(String response) {
            super(response);
        }
    }

    public static final class Unknown extends AuthorizationRequestResponse {
        private Unknown(String response) {
            super(response);
        }
    }
}