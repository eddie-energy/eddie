// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt.message.processor.status;

import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.BaseMessageProcessor;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopicType;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

@Component
public class StatusMessageProcessor extends BaseMessageProcessor {
    private final Sinks.Many<RequestPermissionEnvelope> statusSink;

    public StatusMessageProcessor(
            AiidaPermissionRequestViewRepository permissionRequestViewRepository,
            ObjectMapper objectMapper,
            Sinks.Many<RequestPermissionEnvelope> statusSink
    ) {
        super(permissionRequestViewRepository, objectMapper);
        this.statusSink = statusSink;
    }

    @Override
    public void processMessage(MqttMessage message) {
        var permissionMarketDocument = objectMapper.readValue(message.getPayload(), RequestPermissionEnvelope.class);

        logger.debug("Received permission market document for permission {}",
                     permissionMarketDocument.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
        statusSink.tryEmitNext(permissionMarketDocument);
    }

    @Override
    public String forTopicPath() {
        return MqttTopicType.STATUS.baseTopicName();
    }
}
