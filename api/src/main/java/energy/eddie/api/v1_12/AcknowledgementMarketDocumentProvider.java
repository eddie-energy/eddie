// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v1_12;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of {@link AcknowledgementEnvelope}s available.
 */
public interface AcknowledgementMarketDocumentProvider {
    /**
     * Data stream of all AcknowledgementEnvelope created by this region connector.
     *
     * @return AcknowledgementEnvelope stream
     */
    Flux<AcknowledgementEnvelope> getAcknowledgementDataMarketDocumentsStream();
}
