package energy.eddie.regionconnector.aiida.mqtt.message.processor.data.raw;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.aiida.AiidaRecordDto;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.regionconnector.aiida.exceptions.PermissionInvalidException;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.BaseMessageProcessor;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopicType;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
public class RawDataMessageProcessor extends BaseMessageProcessor {
    private final Sinks.Many<RawDataMessage> rawDataMessageSink;

    public RawDataMessageProcessor(
            AiidaPermissionRequestViewRepository permissionRequestViewRepository,
            ObjectMapper objectMapper,
            Sinks.Many<RawDataMessage> rawDataMessageSink
    ) {
        super(permissionRequestViewRepository, objectMapper);
        this.rawDataMessageSink = rawDataMessageSink;
    }

    @Override
    public void processMessage(
            MqttMessage message
    ) throws PermissionNotFoundException, PermissionInvalidException {
        var aiidaRecordDto = objectMapper.readValue(message.getPayload(), AiidaRecordDto.class);

        var permissionId = aiidaRecordDto.permissionId().toString();
        var permissionRequest = getAndValidatePermissionRequest(permissionId);

        var rawDataMessage = new RawDataMessage(
                permissionRequest,
                new String(message.getPayload(), StandardCharsets.UTF_8)
        );

        logger.debug("Received RawDataMessage for permission {} and AIIDA {}",
                    permissionId,
                    permissionRequest.aiidaId());
        rawDataMessageSink.tryEmitNext(rawDataMessage);
    }

    @Override
    public String forTopicPath() {
        return AiidaSchema.SMART_METER_P1_RAW.buildTopicPath(MqttTopicType.OUTBOUND_DATA.baseTopicName());
    }
}
