// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v1_04;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of {@link RTDEnvelope}s available.
 */
@SuppressWarnings("java:S114")
public interface NearRealTimeDataMarketDocumentProviderV1_04 {
    /**
     * Data stream of all RTDEnvelope created by this region connector.
     *
     * @return RTDEnvelope stream
     */
    Flux<RTDEnvelope> getNearRealTimeDataMarketDocumentsStream();
}
