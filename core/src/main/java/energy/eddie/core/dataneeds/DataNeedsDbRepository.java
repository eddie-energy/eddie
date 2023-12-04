package energy.eddie.core.dataneeds;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Set;

/**
 * Db repository for {@link DataNeedImpl}s.
 */
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
public interface DataNeedsDbRepository extends ListCrudRepository<DataNeedImpl, String> {

    /**
     * Get the ids of all {@link DataNeedImpl}s.
     *
     * @return set containing all (distinct) ids
     */
    @Query("SELECT id FROM #{#entityName}")
    Set<String> findAllIds();
}
