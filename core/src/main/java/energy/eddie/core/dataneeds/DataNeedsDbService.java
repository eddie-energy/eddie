package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataNeedsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * DataNeedService that retrieves data needs from the application database.
 */
@Service
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
public class DataNeedsDbService implements DataNeedsService {

    private final DataNeedsDbRepository repository;

    public DataNeedsDbService(DataNeedsDbRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<DataNeed> getDataNeed(String id) {
        return repository.findById(id).map(dn -> dn);
    }

    @Override
    public Set<String> getAllDataNeedIds() {
        return repository.findAllIds();
    }
}
