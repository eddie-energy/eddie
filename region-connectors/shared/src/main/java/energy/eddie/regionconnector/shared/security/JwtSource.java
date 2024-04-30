package energy.eddie.regionconnector.shared.security;

import org.springframework.http.HttpHeaders;

/**
 *
 */
public enum JwtSource {
    /**
     * Get the JWT from a cookie named {@link JwtUtil#JWT_COOKIE_NAME}.
     */
    COOKIE,
    /**
     * Get the JWT from the {@link HttpHeaders#AUTHORIZATION} header.
     */
    HEADER
}
