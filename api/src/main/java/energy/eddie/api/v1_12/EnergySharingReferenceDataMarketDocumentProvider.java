// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v1_12;

import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import reactor.core.publisher.Flux;

public interface EnergySharingReferenceDataMarketDocumentProvider {
    /**
     * @return Flux of {@link ESRDMDEnvelope}s
     *
     */
    Flux<ESRDMDEnvelope> getEnergySharingReferenceDataMarketDocumentStream();
}
