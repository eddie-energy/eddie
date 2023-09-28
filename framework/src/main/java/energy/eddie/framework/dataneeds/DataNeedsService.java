package energy.eddie.framework.dataneeds;

import java.util.Optional;

/**
 * Service interface for retrieving data needs. Depending on the configuration, different data sources will be used.
 */
public interface DataNeedsService {
    Optional<DataNeed> getDataNeed(String id);
}
