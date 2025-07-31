package energy.eddie.outbound.rest.persistence.cim.v0_82;

import energy.eddie.outbound.rest.model.cim.v0_82.AccountingPointDataMarketDocumentModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountingPointDataMarketDocumentRepository extends JpaRepository<AccountingPointDataMarketDocumentModel, Long>, JpaSpecificationExecutor<AccountingPointDataMarketDocumentModel> {
}
