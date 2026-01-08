package energy.eddie.regionconnector.de.eta.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DeMeterReadingTrackingRepository extends JpaRepository<DeMeterReadingTrackingEntity, Long> {

    // Find by the Permission ID (String)
    Optional<DeMeterReadingTrackingEntity> findByPermissionId(String permissionId);
}