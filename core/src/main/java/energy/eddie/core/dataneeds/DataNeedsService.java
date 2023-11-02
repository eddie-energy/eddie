package energy.eddie.core.dataneeds;

import java.util.Optional;
import java.util.Set;

/**
 * Service interface for retrieving data needs. Depending on the configuration, different data sources will be used.
 */
public interface DataNeedsService {
    Optional<DataNeed> getDataNeed(String id);

    Set<String> getDataNeeds();

    Set<String> getDataNeedGranularities();

    Set<String> getDataNeedTypes();
}
