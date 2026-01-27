package energy.eddie.regionconnector.aiida.mqtt.message.processor.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.regionconnector.aiida.mqtt.message.processor.BaseMessageProcessor;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopicType;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.io.IOException;

@Component
public class StatusMessageProcessor extends BaseMessageProcessor {
    private final Sinks.Many<AiidaConnectionStatusMessageDto> statusSink;

    public StatusMessageProcessor(
            AiidaPermissionRequestViewRepository permissionRequestViewRepository,
            ObjectMapper objectMapper,
            Sinks.Many<AiidaConnectionStatusMessageDto> statusSink
    ) {
        super(permissionRequestViewRepository, objectMapper);
        this.statusSink = statusSink;
    }

    @Override
    public void processMessage(MqttMessage message) throws IOException {
        var statusMessage = objectMapper.readValue(message.getPayload(), AiidaConnectionStatusMessageDto.class);
        var permissionId = statusMessage.permissionId();

        logger.debug("Received connection status message for permission {} with status {}",
                    permissionId,
                    statusMessage.status());
        statusSink.tryEmitNext(statusMessage);
    }

    @Override
    public String forTopicPath() {
        return MqttTopicType.STATUS.baseTopicName();
    }
}
