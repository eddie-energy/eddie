// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.v1_12.outbound.RegionConnectorMinMaxEnvelopeService;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiidaRegionConnectorMinMaxEnvelopeService implements RegionConnectorMinMaxEnvelopeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnectorMinMaxEnvelopeService.class);
    private static final AiidaSchema SCHEMA = AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12;

    private final MqttService mqttService;

    public AiidaRegionConnectorMinMaxEnvelopeService(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @Override
    public void minMaxEnvelopeArrived(RECMMOEEnvelope minMaxEnvelope) {
        var metaInformation = minMaxEnvelope.getMessageDocumentHeader().getMetaInformation();
        var permissionId = metaInformation.getRequestPermissionId();

        LOGGER.info("Received MinMaxEnvelope with permissionId '{}'", permissionId);

        try {
            mqttService.publishInboundData(SCHEMA, permissionId, minMaxEnvelope);
        } catch (MqttException e) {
            LOGGER.error("Failed to publish MinMaxEnvelope with permissionId '{}'", permissionId, e);
        }
    }
}
