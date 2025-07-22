package energy.eddie.outbound.rest.persistence;

import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ConnectionStatusMessageRepository extends JpaRepository<ConnectionStatusMessageModel, Long>, JpaSpecificationExecutor<ConnectionStatusMessageModel> {
}
