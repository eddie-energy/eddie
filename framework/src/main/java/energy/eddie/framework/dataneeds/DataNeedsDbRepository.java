package energy.eddie.framework.dataneeds;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;

/**
 * Db repository for {@link DataNeed}s.
 */
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
public interface DataNeedsDbRepository extends CrudRepository<DataNeed, String> {
}
