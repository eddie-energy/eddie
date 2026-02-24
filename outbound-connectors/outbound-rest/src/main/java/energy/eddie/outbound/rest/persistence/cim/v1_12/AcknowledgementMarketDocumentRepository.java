// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.persistence.cim.v1_12;

import energy.eddie.outbound.rest.model.cim.v1_12.AcknowledgementMarketDocumentModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository(value = "acknowledgementMarketDocumentRepositoryV112")
public interface AcknowledgementMarketDocumentRepository extends JpaRepository<AcknowledgementMarketDocumentModel, Long>, JpaSpecificationExecutor<AcknowledgementMarketDocumentModel> {
}
