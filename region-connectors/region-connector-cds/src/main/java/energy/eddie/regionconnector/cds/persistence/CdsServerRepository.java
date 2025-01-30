package energy.eddie.regionconnector.cds.persistence;

import energy.eddie.regionconnector.cds.permission.administrators.CdsServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CdsServerRepository extends JpaRepository<CdsServer, String> {
}
