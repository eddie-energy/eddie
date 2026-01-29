// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v0_82.outbound;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import reactor.core.publisher.Flux;

/**
 * An outbound connector delivers data to the eligible party. All messages from the region connectors
 * are passed on to the EP using their transport of choice, e.g. Kafka.
 */
public interface ValidatedHistoricalDataEnvelopeOutboundConnector {
    /**
     * Sets the stream of validated historical data market documents to be sent to the EP app.
     *
     * @param marketDocumentStream stream of validated historical data market documents
     */
    void setEddieValidatedHistoricalDataMarketDocumentStream(
            Flux<ValidatedHistoricalDataEnvelope> marketDocumentStream
    );
}