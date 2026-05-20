// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.exceptions;

/**
 * Exceptions thrown by {@link energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient}
 * during API communication with the ETA Plus system.
 *
 * <p>These exceptions represent failure modes of the HTTP client at runtime,
 * as opposed to {@link EtaPlusOperationExceptions} (business-logic / event-flow level).
 * Configuration validation is handled by {@link energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration}.
 */
public final class EtaPlusClientExceptions {

    private EtaPlusClientExceptions() {
    }

    /**
     * Thrown when the ETA Plus API responds with HTTP 401 or 403 due to invalid credentials.
     * This is distinct from a permission-level 403 (where the final customer revoked access) —
     * an {@code AuthenticationException} means our own client credentials are rejected.
     *
     * <p>Retry will not help; the credentials or client configuration must be fixed.
     */
    public static class AuthenticationException extends RuntimeException {
        private final int statusCode;

        public AuthenticationException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public AuthenticationException(String message, int statusCode, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }

    /**
     * Thrown when the ETA Plus API responds with a 5xx server error.
     * This signals a transient condition on the ETA Plus side where a retry strategy
     * (with backoff) is appropriate.
     */
    public static class EtaPlusServerException extends RuntimeException {
        private final int statusCode;

        public EtaPlusServerException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public EtaPlusServerException(String message, int statusCode, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }

    /**
     * Thrown when the ETA Plus API response body cannot be deserialized into the expected DTO.
     * This signals a contract violation between the API response format and our client —
     * automatic retries will not help; the mapping code or API contract must be investigated.
     */
    public static class DeserializationException extends RuntimeException {
        public DeserializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when the HTTP request to ETA Plus times out (connect or response timeout).
     * This is a transient condition where a retry with backoff is appropriate,
     * but repeated timeouts may indicate a systemic issue.
     */
    public static class EtaPlusTimeoutException extends RuntimeException {
        public EtaPlusTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when the ETA Plus API responds with HTTP 400 (Bad Request).
     * Indicates a malformed request from the connector and should be treated as
     * terminal — a retry will not succeed.
     */
    public static class EtaPlusBadRequestException extends RuntimeException {
        private final int statusCode;

        public EtaPlusBadRequestException(String message, int statusCode, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }

    /**
     * Thrown when the ETA Plus API responds with HTTP 403 (Forbidden) on a resource
     * the bearer should normally authorise. Reserved for future per-resource ACL
     * responses; treated as terminal at the handler layer.
     */
    public static class EtaPlusForbiddenException extends RuntimeException {
        private final int statusCode;

        public EtaPlusForbiddenException(String message, int statusCode, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }

    /**
     * Thrown when the ETA Plus API responds with HTTP 404 (Not Found) for a
     * requested resource. Treated as terminal — the resource does not exist.
     */
    public static class EtaPlusNotFoundException extends RuntimeException {
        private final int statusCode;

        public EtaPlusNotFoundException(String message, int statusCode, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }
}
