// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v1_12.outbound;

import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import reactor.core.publisher.Flux;

/**
 * An outbound connector delivers data to the eligible party. All messages from the region connectors
 * are passed on to the EP using their transport of choice, e.g. Kafka.
 */
@SuppressWarnings("java:S114")
public interface AcknowledgementMarketDocumentOutboundConnector {
    @SuppressWarnings("java:S100")
    void setAcknowledgementMarketDocumentStream(
            Flux<AcknowledgementEnvelope> marketDocumentStream
    );
}
