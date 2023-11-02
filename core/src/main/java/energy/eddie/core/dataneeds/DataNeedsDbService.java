package energy.eddie.core.dataneeds;

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
        return repository.findById(id);
    }

    @Override
    public Set<String> getDataNeeds() {
        // TODO: Implement after merge
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Set<String> getDataNeedGranularities() {
        // TODO: Implement after merge
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Set<String> getDataNeedTypes() {
        // TODO: Implement after merge
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
