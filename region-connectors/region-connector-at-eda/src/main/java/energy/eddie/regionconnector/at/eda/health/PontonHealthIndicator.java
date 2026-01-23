// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.health;

import energy.eddie.regionconnector.at.eda.ponton.messenger.MessengerHealth;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PontonHealthIndicator implements HealthIndicator {
    private final MessengerHealth messengerHealth;

    public PontonHealthIndicator(MessengerHealth messengerHealth) {
        this.messengerHealth = messengerHealth;
    }

    @Override
    public Health health() {
        var messengerStatus = messengerHealth.messengerStatus();
        return (messengerStatus.ok() ? Health.up() : Health.down())
                .withDetails(messengerStatus.healthChecks())
                .build();
    }
}
