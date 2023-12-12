package energy.eddie.core.dataneeds;

import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * DataNeedService that retrieves data needs from the spring configuration.
 */
@Service
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "CONFIG")
public class DataNeedsConfigService implements DataNeedsService {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataNeedsConfigService.class);
    private final Map<String, DataNeed> dataNeedForId = new HashMap<>();

    public DataNeedsConfigService(DataNeedsConfig dataNeedsConfig, Validator validator) {
        dataNeedsConfig.getDataNeedForId().values().stream().filter(dataNeed -> {
            var violations = dataNeed.validate(validator);
            if (!violations.isEmpty()) {
                LOGGER.error("Data need {} has validation errors: {}", dataNeed.getId(), violations);
            }
            return violations.isEmpty();
        }).forEach(dataNeed -> dataNeedForId.put(dataNeed.getId(), dataNeed));
        LOGGER.info("Loaded data needs: {}", dataNeedForId.keySet());
    }

    @Override
    public Optional<DataNeed> getDataNeed(String id) {
        return Optional.ofNullable(dataNeedForId.get(id));
    }

    @Override
    public Set<String> getAllDataNeedIds() {
        return dataNeedForId.keySet();
    }

}
