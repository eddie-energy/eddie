// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.config.cleanup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "aiida.cleanup")
public class CleanupConfiguration {
    private Duration interval = Duration.ofDays(1);
    private Map<CleanupEntity, CleanupProperties> entities = new EnumMap<>(CleanupEntity.class);

    public Duration interval() {
        return interval;
    }

    public void setInterval(Duration interval) {
        this.interval = interval;
    }

    public Map<CleanupEntity, CleanupProperties> entities() {
        return entities;
    }

    public void setEntities(Map<CleanupEntity, CleanupProperties> entities) {
        this.entities = entities;
    }

    public record CleanupProperties(Duration retention) {
    }
}
