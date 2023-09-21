package energy.eddie.framework.dataneeds;

import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
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
        private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataNeedsConfig.class);
        private Map<String, DataNeed> dataNeedForId;

        public DataNeedsConfig(List<DataNeed> dataNeeds) {
            this.dataNeedForId = new HashMap<>(dataNeeds.size());
            for (DataNeed dataNeed : dataNeeds) {
                if (dataNeedForId.containsKey(dataNeed.id())) {
                    LOGGER.error("Duplicate data need id read from spring config, id: {}", dataNeed.id());
                }
                dataNeedForId.put(dataNeed.id(), dataNeed);
            }
        }

        public Map<String, DataNeed> getDataNeedForId() {
            return dataNeedForId;
        }
    }
}
