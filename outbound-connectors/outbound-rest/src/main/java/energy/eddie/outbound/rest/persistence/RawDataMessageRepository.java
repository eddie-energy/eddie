package energy.eddie.outbound.rest.persistence;

import energy.eddie.outbound.rest.model.RawDataMessageModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RawDataMessageRepository extends JpaRepository<RawDataMessageModel, Long>, JpaSpecificationExecutor<RawDataMessageModel> {
}
