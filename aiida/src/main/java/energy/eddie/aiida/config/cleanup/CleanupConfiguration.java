package energy.eddie.aiida.config.cleanup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "aiida.cleanup")
public class CleanupConfiguration {
    private Duration cleanupInterval = Duration.ofDays(1);
    private Map<CleanupEntity, CleanupProperties> entities = new EnumMap<>(CleanupEntity.class);

    public Duration cleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
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
