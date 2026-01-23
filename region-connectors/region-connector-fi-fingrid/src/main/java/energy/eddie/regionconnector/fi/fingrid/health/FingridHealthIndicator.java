// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.health;

import energy.eddie.regionconnector.fi.fingrid.client.FingridApiClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FingridHealthIndicator implements HealthIndicator {
    private final FingridApiClient api;

    public FingridHealthIndicator(FingridApiClient api) {this.api = api;}

    @Override
    public Health health() {
        return api.health();
    }
}
