package energy.eddie.regionconnector.de.eta.oauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory store for OAuth states. Intended for tests; production can replace with DB-backed repo.
 */
@Service
public class DeEtaOAuthStateStore {
    private final Map<String, DeEtaOAuthState> states = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;

    @Autowired
    public DeEtaOAuthStateStore() {
        this(Clock.systemUTC(), Duration.ofMinutes(10));
    }

    public DeEtaOAuthStateStore(Clock clock, Duration ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    public DeEtaOAuthState save(UUID permissionId, String connectionId, String state) {
        cleanupExpired();
        Instant now = clock.instant();
        var stateRecord = new DeEtaOAuthState(state, permissionId, connectionId, now, now.plus(ttl));
        states.put(state, stateRecord);
        return stateRecord;
    }

    public Optional<DeEtaOAuthState> find(String state) {
        cleanupExpired();
        return Optional.ofNullable(states.get(state))
                .filter(s -> s.expiresAt().isAfter(clock.instant()));
    }

    private void cleanupExpired() {
        Instant now = clock.instant();
        states.values().removeIf(s -> s.expiresAt().isBefore(now));
    }
}
