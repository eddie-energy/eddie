// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v1_12.outbound;

import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import reactor.core.publisher.Flux;

/**
 * A min-max envelope connector allows the eligible party to send reference energy curve min-max operating envelopes to a certain region-connector.
 */
public interface MinMaxEnvelopeOutboundConnector {
    /**
     * A flux of {@link RECMMOEEnvelope}s, which provide a min-max envelope curve for a certain time period.
     *
     * @return A flux of min-max envelopes to be sent to the region-connector.
     */
    Flux<RECMMOEEnvelope> getMinMaxEnvelopes();
}
