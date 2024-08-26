package energy.eddie.dataneeds.persistence;

import energy.eddie.dataneeds.needs.DataNeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataNeedsRepository extends JpaRepository<DataNeed, String> {
    List<DataNeedsNameAndIdProjection> findAllBy();

    @Modifying
    @Query("update DataNeed dn set dn.enabled = :enabled where dn.id = :id")
    void setEnabledById(@Param("id") String id, @Param("enabled") boolean enabled);
}
