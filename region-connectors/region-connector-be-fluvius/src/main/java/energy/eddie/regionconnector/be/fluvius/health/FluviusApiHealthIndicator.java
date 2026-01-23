// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.health;

import energy.eddie.regionconnector.be.fluvius.client.FluviusApiClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FluviusApiHealthIndicator implements HealthIndicator {
    private final FluviusApiClient api;

    public FluviusApiHealthIndicator(FluviusApiClient api) {
        this.api = api;
    }

    @Override
    public Health health() {
        return api.health();
    }
}
