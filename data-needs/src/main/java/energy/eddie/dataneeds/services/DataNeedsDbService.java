package energy.eddie.dataneeds.services;

import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;
import energy.eddie.dataneeds.persistence.DataNeedsRepository;
import energy.eddie.dataneeds.utils.DataNeedUtils;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
public class DataNeedsDbService implements DataNeedsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataNeedsDbService.class);
    private final DataNeedsRepository repository;

    public DataNeedsDbService(DataNeedsRepository repository) {
        this.repository = repository;
        LOGGER.info("Initialized database data needs service.");
    }

    @Override
    public List<DataNeedsNameAndIdProjection> getDataNeedIdsAndNames() {
        return repository.findAllBy();
    }

    @Override
    public Optional<DataNeed> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public DataNeedWrapper findDataNeedAndCalculateStartAndEnd(
            String dataNeedId,
            LocalDate referenceDate,
            Period earliestStart,
            Period latestEnd
    ) throws DataNeedNotFoundException, IllegalArgumentException {
        DataNeed dataNeed = repository.findById(dataNeedId)
                                      .orElseThrow(() -> new DataNeedNotFoundException(dataNeedId, true));
        return DataNeedUtils.calculateRelativeStartAndEnd(dataNeed, referenceDate, earliestStart, latestEnd);
    }

    /**
     * Saves the new data need in the database and sets a UUID as {@link DataNeed#id()}.
     *
     * @param newDataNeed Data need to save in the database.
     * @return Persisted data need.
     */
    public DataNeed saveNewDataNeed(DataNeed newDataNeed) {
        newDataNeed.setId(UUID.randomUUID().toString());

        if (newDataNeed instanceof TimeframedDataNeed timeframedDataNeed)
            timeframedDataNeed.duration().setDataNeedId(newDataNeed.id());

        LOGGER.info("Saving new data need with ID '{}'", newDataNeed.id());

        return repository.save(newDataNeed);
    }

    /**
     * Returns a list of all data needs saved in the database.
     */
    public List<DataNeed> findAll() {
        return repository.findAll();
    }

    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    public void deleteById(String id) {
        LOGGER.debug("Deleting data need with ID {}", id);
        repository.deleteById(id);
    }
}
