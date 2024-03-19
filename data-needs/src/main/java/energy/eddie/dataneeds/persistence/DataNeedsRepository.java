package energy.eddie.dataneeds.persistence;

import energy.eddie.dataneeds.needs.DataNeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataNeedsRepository extends JpaRepository<DataNeed, String> {
    List<DataNeedsNameAndIdProjection> findAllBy();
}
