package energy.eddie.outbound.rest.persistence.cim.v1_06;

import energy.eddie.outbound.rest.model.cim.v1_06.NearRealTimeDataMarketDocumentModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository(value = "nearRealTimeDataMarketDocumentRepositoryV106")
public interface NearRealTimeDataMarketDocumentRepository extends JpaRepository<NearRealTimeDataMarketDocumentModel, Long>, JpaSpecificationExecutor<NearRealTimeDataMarketDocumentModel> {
}
