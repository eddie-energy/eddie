// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.v1_12.outbound.RegionConnectorMinMaxEnvelopeService;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiidaRegionConnectorMinMaxEnvelopeService implements RegionConnectorMinMaxEnvelopeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnectorMinMaxEnvelopeService.class);

    @Override
    public void minMaxEnvelopeArrived(RECMMOEEnvelope minMaxEnvelope) {
        LOGGER.info("Received MinMaxEnvelope: {}", minMaxEnvelope);
        // TODO: GH-2125 forward to AIIDA
    }
}
