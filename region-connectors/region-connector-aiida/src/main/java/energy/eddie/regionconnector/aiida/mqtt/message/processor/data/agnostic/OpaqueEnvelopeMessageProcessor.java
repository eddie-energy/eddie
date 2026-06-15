// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt.message.processor.data.agnostic;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
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
public class OpaqueEnvelopeMessageProcessor extends BaseMessageProcessor {
    private final Sinks.Many<OpaqueEnvelope> opaqueEnvelopeSink;

    public OpaqueEnvelopeMessageProcessor(
            AiidaPermissionRequestViewRepository permissionRequestViewRepository,
            ObjectMapper objectMapper,
            Sinks.Many<OpaqueEnvelope> opaqueEnvelopeSink
    ) {
        super(permissionRequestViewRepository, objectMapper);
        this.opaqueEnvelopeSink = opaqueEnvelopeSink;
    }

    @Override
    public void processMessage(MqttMessage message) throws PermissionNotFoundException, PermissionInvalidException {
        var opaqueEnvelope = objectMapper.readValue(message.getPayload(), OpaqueEnvelope.class);
        var permissionId = opaqueEnvelope.permissionId();
        getAndValidatePermissionRequest(permissionId);

        logger.debug("Received opaque envelope for permission {}", permissionId);
        opaqueEnvelopeSink.tryEmitNext(opaqueEnvelope);
    }

    @Override
    public String forTopicPath() {
        return AiidaSchema.OPAQUE.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
    }
}
