package energy.eddie.regionconnector.de.eta.oauth;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a persisted OAuth state mapping to a permissionId and connectionId with TTL.
 */
public record DeEtaOAuthState(
        String state,
        UUID permissionId,
        String connectionId,
        Instant createdAt,
        Instant expiresAt
) {}
