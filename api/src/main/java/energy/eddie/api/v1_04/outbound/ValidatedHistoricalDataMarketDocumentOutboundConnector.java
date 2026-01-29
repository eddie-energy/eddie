// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v1_04.outbound;

import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import reactor.core.publisher.Flux;

/**
 * An outbound connector that takes a stream of {@link VHDEnvelope}s.
 * @see energy.eddie.api.v0_82.outbound.ValidatedHistoricalDataEnvelopeOutboundConnector
 */
public interface ValidatedHistoricalDataMarketDocumentOutboundConnector {

    void setValidatedHistoricalDataMarketDocumentStream(
            Flux<VHDEnvelope> marketDocumentStream
    );
}
