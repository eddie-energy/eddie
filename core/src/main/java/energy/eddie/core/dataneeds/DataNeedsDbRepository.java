package energy.eddie.core.dataneeds;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Set;

/**
 * Db repository for {@link DataNeed}s.
 */
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
public interface DataNeedsDbRepository extends CrudRepository<DataNeed, String> {

    /**
     * Get the ids of all {@link DataNeed}s.
     *
     * @return set containing all (distinct) ids
     */
    @Query("SELECT dn.id FROM DataNeed dn")
    Set<String> findAllIds();
}
