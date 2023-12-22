package energy.eddie.api.agnostic;

import java.util.Optional;
import java.util.Set;

/**
 * Service interface for retrieving data needs. Depending on the configuration, different data sources will be used.
 */
public interface DataNeedsService {

    /**
     * Retrieve a data need by its id.
     *
     * @param id the id of the data need
     * @return the data need or an empty optional if there is no data need with the given id
     */
    Optional<DataNeed> getDataNeed(String id);

    /**
     * Get all available data need ids.
     *
     * @return all data need ids
     */
    Set<String> getAllDataNeedIds();
}
