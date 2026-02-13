// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.persistence.cim.v1_04;

import energy.eddie.outbound.rest.model.cim.v1_04.NearRealTimeDataMarketDocumentModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository(value = "nearRealTimeDataMarketDocumentRepositoryV104")
public interface NearRealTimeDataMarketDocumentRepository extends JpaRepository<NearRealTimeDataMarketDocumentModel, Long>, JpaSpecificationExecutor<NearRealTimeDataMarketDocumentModel> {
}
