package energy.eddie.framework.dataneeds;

import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for the data need specific spring configuration. Ensures that data needs are either read from
 * the application database or the spring configuration.
 */
@ConfigurationProperties(prefix = "eddie.data-needs-config")
public class DataNeedsConfig {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataNeedsConfig.class);
    private final Map<String, DataNeed> dataNeedForId;

    public DataNeedsConfig(List<DataNeed> dataNeeds, DataNeedSource dataNeedSource) {
        if (dataNeedSource == DataNeedSource.DATABASE && (null != dataNeeds && !dataNeeds.isEmpty())) {
            throw new IllegalConfigurationException("There must not be any data needs in the config when using dataNeedSource: " + DataNeedSource.DATABASE);
        }
        if (null != dataNeeds) {
            this.dataNeedForId = HashMap.newHashMap(dataNeeds.size());
            for (DataNeed dataNeed : dataNeeds) {
                if (dataNeedForId.containsKey(dataNeed.getId())) {
                    LOGGER.error("Duplicate data need id read from spring config, id: {}", dataNeed.getId());
                }
                dataNeedForId.put(dataNeed.getId(), dataNeed);
            }
        } else {
            this.dataNeedForId = new HashMap<>();
        }
    }

    public Map<String, DataNeed> getDataNeedForId() {
        return dataNeedForId;
    }

    public enum DataNeedSource {
        DATABASE, CONFIG
    }

    public static class IllegalConfigurationException extends RuntimeException {
        public IllegalConfigurationException(String s) {
            super(s);
        }
    }
}
