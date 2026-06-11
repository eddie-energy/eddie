// SPDX-FileCopyrightText: 2026 The ETA+ Developers <bilal.sakhawat@etaplus.energy>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.health;

import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Health indicator reporting the reachability of the ETA Plus PA/MDA API.
 * Delegates to {@link EtaPlusApiClient#isAlive()}: a reachable API is reported as
 * {@code UP}, an unreachable one (5xx, connection failure or timeout) as {@code DOWN}.
 */
@Component
public class EtaPlusApiHealthIndicator implements HealthIndicator {
    private final EtaPlusApiClient api;

    public EtaPlusApiHealthIndicator(EtaPlusApiClient api) {
        this.api = api;
    }

    @Override
    public Health health() {
        return api.isAlive()
                  .map(isAlive -> Boolean.TRUE.equals(isAlive) ? Health.up() : Health.down())
                  .onErrorResume(Exception.class, e -> Mono.just(Health.down(e)))
                  .onErrorResume(e -> Mono.just(Health.down()))
                  .map(Health.Builder::build)
                  .block();
    }
}