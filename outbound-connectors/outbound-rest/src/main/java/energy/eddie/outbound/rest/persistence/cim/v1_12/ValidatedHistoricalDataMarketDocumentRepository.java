// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.persistence.cim.v1_12;

import energy.eddie.outbound.rest.model.cim.v1_12.ValidatedHistoricalDataMarketDocumentModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository("validatedHistoricalDataMarketDocumentRepositoryV112")
public interface ValidatedHistoricalDataMarketDocumentRepository extends JpaRepository<ValidatedHistoricalDataMarketDocumentModel, Long>, JpaSpecificationExecutor<ValidatedHistoricalDataMarketDocumentModel> {
}
