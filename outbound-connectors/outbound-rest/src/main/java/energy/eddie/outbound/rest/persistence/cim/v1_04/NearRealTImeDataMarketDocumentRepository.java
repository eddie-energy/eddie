package energy.eddie.outbound.rest.persistence.cim.v1_04;

import energy.eddie.outbound.rest.model.cim.v1_04.NearRealTimeDataMarketDocumentModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NearRealTImeDataMarketDocumentRepository extends JpaRepository<NearRealTimeDataMarketDocumentModel, Long>, JpaSpecificationExecutor<NearRealTimeDataMarketDocumentModel> {
}
