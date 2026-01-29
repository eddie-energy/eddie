// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.health;

import energy.eddie.regionconnector.fr.enedis.api.EnedisHealth;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

public class EnedisApiHealthIndicator implements HealthIndicator {
    private final EnedisHealth enedisHealth;
    private final String apiName;

    EnedisApiHealthIndicator(EnedisHealth enedisHealth, String apiName) {
        this.enedisHealth = enedisHealth;
        this.apiName = apiName;
    }

    @Override
    public Health health() {
        return enedisHealth.health().getOrDefault(apiName, Health.unknown().build());
    }
}
