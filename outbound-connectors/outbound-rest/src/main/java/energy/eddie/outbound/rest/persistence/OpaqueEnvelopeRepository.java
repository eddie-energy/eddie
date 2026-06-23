// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.persistence;

import energy.eddie.outbound.rest.model.OpaqueEnvelopeModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface OpaqueEnvelopeRepository extends JpaRepository<OpaqueEnvelopeModel, Long>, JpaSpecificationExecutor<OpaqueEnvelopeModel> {
}
