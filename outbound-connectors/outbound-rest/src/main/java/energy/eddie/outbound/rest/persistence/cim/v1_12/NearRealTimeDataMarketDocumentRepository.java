package energy.eddie.outbound.rest.persistence.cim.v1_12;

import energy.eddie.outbound.rest.model.cim.v1_12.NearRealTimeDataMarketDocumentModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository(value = "nearRealTimeDataMarketDocumentRepositoryV112")
public interface NearRealTimeDataMarketDocumentRepository extends JpaRepository<NearRealTimeDataMarketDocumentModel, Long>, JpaSpecificationExecutor<NearRealTimeDataMarketDocumentModel> {
}
