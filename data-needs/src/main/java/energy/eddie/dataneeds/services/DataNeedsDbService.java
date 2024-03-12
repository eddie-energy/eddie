package energy.eddie.dataneeds.services;

import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;
import energy.eddie.dataneeds.persistence.DataNeedsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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
}
