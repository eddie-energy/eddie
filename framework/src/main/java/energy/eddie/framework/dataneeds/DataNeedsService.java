package energy.eddie.framework.dataneeds;

import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Map;

@Service
public class DataNeedsService {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataNeedsService.class);
    private DataNeedsConfig dataNeedsConfig;

    public DataNeedsService(DataNeedsConfig dataNeedsConfig) {
        this.dataNeedsConfig = dataNeedsConfig;
        LOGGER.info("Loaded data needs: {}", dataNeedsConfig.getDataNeedForId().keySet());
    }

    @Nullable
    public DataNeed getDataNeed(String id) {
        return dataNeedsConfig.getDataNeedForId().get(id);
    }

    @ConfigurationProperties(prefix = "eddie.data-needs-config")
    public static class DataNeedsConfig {
        private Map<String, DataNeed> dataNeedForId;

        public DataNeedsConfig(Map<String, DataNeed> dataNeedForId) {
            this.dataNeedForId = dataNeedForId;
        }

        public Map<String, DataNeed> getDataNeedForId() {
            return dataNeedForId;
        }
    }
}
