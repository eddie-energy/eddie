// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v1_12.outbound;

import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import reactor.core.publisher.Flux;

/**
 * An energy sharing reference data market document outbound connector allows the eligible party to receive energy sharing reference data market documents from certain region-connectors.
 */
public interface EnergySharingReferenceDataMarketDocumentOutboundConnector {
    /**
     * @param marketDocumentStream - Flux of {@link energy.eddie.cim.v1_12.esr.ESRDMDEnvelope}s
     *
     */
    void setEnergySharingReferenceDataMarketDocumentStream(
            Flux<ESRDMDEnvelope> marketDocumentStream
    );
}
