package energy.eddie.regionconnector.de.eta.oauth;

import java.util.Optional;

public record OAuthCallback(Optional<String> code, Optional<String> error, String state) {

    /**
     * Either code or error must be provided as well as state.
     *
     * @param code  When the user has authorized the application, the authorization
     *              server will redirect the user back
     *              to the application with an authorization code.
     * @param error When the user denies the application or there is an error, the
     *              authorization server will redirect
     *              the user back to the application with an error.
     * @param state Is the permissionId of the permission that was created when the
     *              user started the authorization
     *              process.
     */
    public OAuthCallback {
        if (code.isEmpty() && error.isEmpty()) {
            throw new IllegalArgumentException("Either code or error must be provided");
        } else if (code.isPresent() && error.isPresent()) {
            throw new IllegalArgumentException("Only one of code or error must be provided");
        }
    }

    public boolean isSuccessful() {
        return code.isPresent();
    }
}
