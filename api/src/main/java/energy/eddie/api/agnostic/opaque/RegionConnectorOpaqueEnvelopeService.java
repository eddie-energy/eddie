// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.opaque;

public interface RegionConnectorOpaqueEnvelopeService {
    /**
     * This method is called when an opaque envelope arrives at the region connector.
     * The implementation should handle the message accordingly.
     *
     * @param opaqueEnvelope The opaque envelope that has arrived.
     */
    void opaqueEnvelopeArrived(OpaqueEnvelope opaqueEnvelope);
}
