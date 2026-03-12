// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.services.inbound;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.opaque.OpaqueEnvelope;
import energy.eddie.api.agnostic.opaque.RegionConnectorOpaqueEnvelopeService;
import energy.eddie.regionconnector.aiida.services.MqttService;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiidaRegionConnectorOpaqueEnvelopeService implements RegionConnectorOpaqueEnvelopeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnectorOpaqueEnvelopeService.class);
    private static final AiidaSchema SCHEMA = AiidaSchema.OPAQUE;

    private final MqttService mqttService;

    public AiidaRegionConnectorOpaqueEnvelopeService(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @Override
    public void opaqueEnvelopeArrived(OpaqueEnvelope opaqueEnvelope) {
        var permissionId = opaqueEnvelope.permissionId();

        LOGGER.info("Received opaque message with permissionId '{}'", permissionId);

        try {
            mqttService.publishInboundData(SCHEMA, permissionId, opaqueEnvelope);
        } catch (MqttException e) {
            LOGGER.error("Failed to publish opaque message with permissionId '{}'", permissionId, e);
        }
    }
}
