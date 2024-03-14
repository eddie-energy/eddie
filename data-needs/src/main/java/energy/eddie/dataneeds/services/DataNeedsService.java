package energy.eddie.dataneeds.services;

import energy.eddie.api.utils.Shared;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;

import java.util.List;
import java.util.Optional;


/**
 * Service interface for retrieving data needs. Depending on the configuration, different data sources (e.g. JSON file
 * or database) will be used.
 */
@Shared
public interface DataNeedsService {

    /**
     * Returns a list of all data needs with only their name and ID.
     */
    List<DataNeedsNameAndIdProjection> getDataNeedIdsAndNames();

    /**
     * Fetches the data need with the passed ID from the underlying persistence layer.
     *
     * @param id The ID of the data need to get.
     * @return The data need or an empty Optional if there is no data need with the given ID.
     */
    Optional<DataNeed> findById(String id);
}
