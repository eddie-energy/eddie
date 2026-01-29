// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v1_04;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of {@link VHDEnvelope}s available.
 */
public interface ValidatedHistoricalDataMarketDocumentProvider {
    /**
     * Data stream of all VHDEnvelope created by this region connector.
     *
     * @return VHDEnvelope stream
     */
    Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream();
}
