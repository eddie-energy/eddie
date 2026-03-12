// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.outbound;

import energy.eddie.api.agnostic.opaque.OpaqueEnvelope;
import reactor.core.publisher.Flux;

/**
 * An opaque envelope connector allows the eligible party to send opaque envelopes with any payload to a certain region-connector.
 */
public interface OpaqueEnvelopeOutboundConnector {

    /**
     * A flux of {@link OpaqueEnvelope}s, which provide an opaque envelopes with any payload for a certain time period.
     *
     * @return A flux of opaque envelopes to be sent to the region-connector.
     */
    Flux<OpaqueEnvelope> getOpaqueEnvelopes();
}
