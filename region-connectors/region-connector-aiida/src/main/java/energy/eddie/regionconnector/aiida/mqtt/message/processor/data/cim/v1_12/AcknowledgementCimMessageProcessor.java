// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt.message.processor.data.cim.v1_12;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.regionconnector.aiida.exceptions.PermissionInvalidException;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.BaseMessageProcessor;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopicType;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

@Component(value = "acknowledgementCimMessageProcessor")
public class AcknowledgementCimMessageProcessor extends BaseMessageProcessor {
    private final Sinks.Many<AcknowledgementEnvelope> acknowledgementCimSink;

    public AcknowledgementCimMessageProcessor(
            AiidaPermissionRequestViewRepository permissionRequestViewRepository,
            ObjectMapper objectMapper,
            Sinks.Many<AcknowledgementEnvelope> acknowledgementCimSink
    ) {
        super(permissionRequestViewRepository, objectMapper);
        this.acknowledgementCimSink = acknowledgementCimSink;
    }

    @Override
    public void processMessage(MqttMessage message) throws PermissionNotFoundException, PermissionInvalidException {
        var acknowledgementEnvelope = objectMapper.readValue(message.getPayload(), AcknowledgementEnvelope.class);
        var metaInformation = acknowledgementEnvelope.getMessageDocumentHeader().getMetaInformation();

        var permissionId = metaInformation.getRequestPermissionId();
        getAndValidatePermissionRequest(permissionId);

        logger.debug("Received acknowledgement market document for permission {} and final customer {}",
                    permissionId,
                    metaInformation.getFinalCustomerId());
        acknowledgementCimSink.tryEmitNext(acknowledgementEnvelope);
    }

    @Override
    public String forTopicPath() {
        return AiidaSchema.ACKNOWLEDGEMENT_CIM_V1_12.buildTopicPath(MqttTopicType.ACKNOWLEDGEMENT.baseTopicName());
    }
}
