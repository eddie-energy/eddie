// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt.message.processor.data.cim.v1_12;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import energy.eddie.regionconnector.aiida.exceptions.PermissionInvalidException;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.BaseMessageProcessor;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopicType;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

@Component(value = "cimDataMessageProcessorV112")
public class CimDataMessageProcessor extends BaseMessageProcessor {
    private final Sinks.Many<RTDEnvelope> nearRealTimeDataSink;

    public CimDataMessageProcessor(
            AiidaPermissionRequestViewRepository permissionRequestViewRepository,
            ObjectMapper objectMapper,
            Sinks.Many<RTDEnvelope> nearRealTimeDataSink
    ) {
        super(permissionRequestViewRepository, objectMapper);
        this.nearRealTimeDataSink = nearRealTimeDataSink;
    }

    @Override
    public void processMessage(MqttMessage message) throws PermissionNotFoundException, PermissionInvalidException {
        var nearRealTimeDataEnvelope = objectMapper.readValue(message.getPayload(), RTDEnvelope.class);
        var metaInformation = nearRealTimeDataEnvelope.getMessageDocumentHeader().getMetaInformation();

        var permissionId = metaInformation.getRequestPermissionId();
        getAndValidatePermissionRequest(permissionId);

        logger.debug("Received near real-time data market document for permission {} and final customer {}",
                    permissionId,
                    metaInformation.getFinalCustomerId());
        nearRealTimeDataSink.tryEmitNext(nearRealTimeDataEnvelope);
    }

    @Override
    public String forTopicPath() {
        return AiidaSchema.SMART_METER_P1_CIM_V1_12.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
    }
}
