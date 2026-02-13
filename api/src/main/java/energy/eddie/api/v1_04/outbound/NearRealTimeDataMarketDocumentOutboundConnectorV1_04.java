// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v1_04.outbound;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import reactor.core.publisher.Flux;

/**
 * An outbound connector delivers data to the eligible party. All messages from the region connectors
 * are passed on to the EP using their transport of choice, e.g. Kafka.
 */
@SuppressWarnings("java:S114")
public interface NearRealTimeDataMarketDocumentOutboundConnectorV1_04 {
    @SuppressWarnings("java:S100")
    void setNearRealTimeDataMarketDocumentStreamV1_04(Flux<RTDEnvelope> marketDocumentStream);
}
