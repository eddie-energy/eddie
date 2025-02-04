package energy.eddie.regionconnector.cds.persistence;

import energy.eddie.regionconnector.cds.master.data.CdsServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CdsServerRepository extends JpaRepository<CdsServer, Long> {
    Optional<CdsServer> findByBaseUri(String baseUri);
}
