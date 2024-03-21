package energy.eddie.dataneeds.services;

import energy.eddie.api.utils.Shared;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;
import energy.eddie.dataneeds.utils.DataNeedWrapper;

import java.time.LocalDate;
import java.time.Period;
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

    /**
     * Gets the data need from the underlying storage layer and returns the start and end date that should be used for a
     * new permission request with the specified data need. Both dates are inclusive.
     * <p>
     * Evaluates the relative dates if the data need with the passed ID has a relative duration. It uses
     * {@code referenceDate} to calculate the start and end dates, therefore the date should have been created with the
     * timezone of the region connector in mind. {@code earliestStart} and {@code latestEnd} represent the Periods for
     * the earliest/latest date the calling region connector supports and are used if the data need is configured with
     * open start/end. This method also calculates the correct start date if a sticky start is defined by the data needs
     * relative duration.
     *
     * @param id            ID of the data need.
     * @param referenceDate Reference date to use as reference for calculations for relative durations.
     * @param earliestStart Period indicating the earliest start that is supported by the calling region connector.
     * @param latestEnd     Period indicating the latest end that is supported by the calling region connector.
     * @return Wrapper containing the data need and the start and end date.
     * @throws DataNeedNotFoundException If no data need with the provided ID exists.
     * @throws IllegalArgumentException  If the data need with the provided ID does not have a duration.
     */
    DataNeedWrapper findDataNeedAndCalculateStartAndEnd(
            String id,
            LocalDate referenceDate,
            Period earliestStart,
            Period latestEnd
    ) throws DataNeedNotFoundException;
}
