// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.persistence.cim.v1_04;

import energy.eddie.outbound.rest.model.cim.v1_04.ValidatedHistoricalDataMarketDocumentModelV1_04;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@SuppressWarnings("java:S114")
public interface ValidatedHistoricalDataMarketDocumentV1_04Repository extends JpaRepository<ValidatedHistoricalDataMarketDocumentModelV1_04, Long>, JpaSpecificationExecutor<ValidatedHistoricalDataMarketDocumentModelV1_04> {
}
