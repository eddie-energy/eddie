package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataNeedsService;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * DataNeedService that retrieves data needs from the spring configuration.
 */
@Service
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "CONFIG")
public class DataNeedsConfigService implements DataNeedsService {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataNeedsConfigService.class);
    private final DataNeedsConfig dataNeedsConfig;

    public DataNeedsConfigService(DataNeedsConfig dataNeedsConfig) {
        this.dataNeedsConfig = dataNeedsConfig;
        LOGGER.info("Loaded data needs: {}", dataNeedsConfig.getDataNeedForId().keySet());
    }

    @Override
    public Optional<DataNeed> getDataNeed(String id) {
        return Optional.ofNullable(dataNeedsConfig.getDataNeedForId().get(id));
    }

    @Override
    public Set<String> getAllDataNeedIds() {
        return dataNeedsConfig.getDataNeedForId().keySet();
    }

}
