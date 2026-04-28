// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.exceptions;

public final class EtaPlusOperationExceptions {

    private EtaPlusOperationExceptions() {
    }

    /**
     * Thrown when the ETA Plus API responds with HTTP 429 (Too Many Requests).
     * This signals a temporary condition where a backoff/retry strategy should be applied
     * rather than aborting the request permanently.
     */
    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when a permission request cannot be found in the DE-ETA repository.
     * This is an unchecked exception suitable for use in reactive/event-driven flows
     * where checked exceptions cannot be propagated.
     */
    public static class PermissionNotFoundException extends RuntimeException {
        public PermissionNotFoundException(String permissionId) {
            super("No DE-ETA permission request found for id: " + permissionId);
        }
    }

    /**
     * Thrown when persisting an event to the outbox fails.
     * This indicates a critical system state where the event store is out of sync
     * with the actual permission state and requires immediate attention.
     */
    public static class OutboxPublishingException extends RuntimeException {
        public OutboxPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when the ETA Plus API response cannot be mapped to the domain model.
     * This signals a contract violation between the API and the client —
     * automatic retries will not help; the mapping code or API must be adjusted.
     */
    public static class PayloadMappingException extends RuntimeException {
        public PayloadMappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
