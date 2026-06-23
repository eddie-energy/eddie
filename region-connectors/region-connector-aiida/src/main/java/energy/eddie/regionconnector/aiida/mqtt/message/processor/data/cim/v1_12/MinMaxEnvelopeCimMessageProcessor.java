// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt.message.processor.data.cim.v1_12;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.regionconnector.aiida.exceptions.PermissionInvalidException;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.BaseMessageProcessor;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopicType;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

@Component
public class MinMaxEnvelopeCimMessageProcessor extends BaseMessageProcessor {
    private final Sinks.Many<RECMMOEEnvelope> minMaxEnvelopeSink;

    public MinMaxEnvelopeCimMessageProcessor(
            AiidaPermissionRequestViewRepository permissionRequestViewRepository,
            ObjectMapper objectMapper,
            Sinks.Many<RECMMOEEnvelope> minMaxEnvelopeSink
    ) {
        super(permissionRequestViewRepository, objectMapper);
        this.minMaxEnvelopeSink = minMaxEnvelopeSink;
    }

    @Override
    public void processMessage(MqttMessage message) throws PermissionNotFoundException, PermissionInvalidException {
        var minMaxEnvelope = objectMapper.readValue(message.getPayload(), RECMMOEEnvelope.class);
        var metaInformation = minMaxEnvelope.getMessageDocumentHeader().getMetaInformation();

        var permissionId = metaInformation.getRequestPermissionId();
        getAndValidatePermissionRequest(permissionId);

        logger.debug("Received min-max envelope for permission {} and final customer {}",
                     permissionId,
                     metaInformation.getFinalCustomerId());
        minMaxEnvelopeSink.tryEmitNext(minMaxEnvelope);
    }

    @Override
    public String forTopicPath() {
        return AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
    }
}
