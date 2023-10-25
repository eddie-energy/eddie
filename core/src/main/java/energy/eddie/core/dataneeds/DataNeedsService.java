package energy.eddie.core.dataneeds;

import energy.eddie.api.v0.ConsumptionRecord;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataNeedsService {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataNeedsService.class);
    private final DataNeedsConfig dataNeedsConfig;

    public DataNeedsService(DataNeedsConfig dataNeedsConfig) {
        this.dataNeedsConfig = dataNeedsConfig;
        LOGGER.info("Loaded data needs: {}", dataNeedsConfig.getDataNeedForId().keySet());
    }

    @Nullable
    public DataNeed getDataNeed(String id) {
        return dataNeedsConfig.getDataNeedForId().get(id);
    }

    public Set<String> getDataNeeds() {
        return dataNeedsConfig.getDataNeedForId().keySet();
    }

    public Set<String> getDataNeedTypes() {
        return Arrays.stream(DataType.values()).map(Enum::toString).collect(Collectors.toSet());
    }

    public Set<String> getDataNeedGranularities() {
        return Arrays.stream(ConsumptionRecord.MeteringInterval.values()).map(Enum::toString).collect(Collectors.toSet());
    }

    @ConfigurationProperties(prefix = "eddie.data-needs-config")
    public static class DataNeedsConfig {
        private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataNeedsConfig.class);
        private final Map<String, DataNeed> dataNeedForId;

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
