// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v1_12.outbound;

import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;

public interface RegionConnectorMinMaxEnvelopeService {
    void minMaxEnvelopeArrived(RECMMOEEnvelope minMaxEnvelope);
}
