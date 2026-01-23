// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.health;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DatadisApiHealthIndicator implements HealthIndicator {
    private Health status = Health.unknown().build();

    public void up() {
        status = Health.up().build();
    }

    public void down(Throwable throwable) {
        if (throwable instanceof IOException e) {
            status = Health.down(e).build();
        } else if (throwable instanceof DatadisApiException apiException && apiException.statusCode() >= 500 && apiException.statusCode() < 600) {
            status = Health.down(apiException).build();
        }
    }

    @Override
    public Health health() {
        return status;
    }
}
