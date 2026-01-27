package energy.eddie.regionconnector.aiida.mqtt.message.processor;

import energy.eddie.regionconnector.aiida.exceptions.AiidaMessageProcessorRegistryException;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiidaMessageProcessorRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaMessageProcessorRegistry.class);
    private static final int MESSAGE_VERSION_LENGTH = MqttTopic.defaultPrefix().length();
    private static final int PERMISSION_ID_LENGTH = 36;
    private static final int DELIMITER_LENGTH = "/".length();

    private final Map<String, AiidaMessageProcessor> messageProcessors = new HashMap<>();

    public AiidaMessageProcessorRegistry(List<AiidaMessageProcessor> aiidaMessageProcessors) {
        for (AiidaMessageProcessor aiidaMessageProcessor : aiidaMessageProcessors) {
            var topicSuffix = aiidaMessageProcessor.forTopicPath();
            messageProcessors.putIfAbsent(topicSuffix, aiidaMessageProcessor);
        }

        LOGGER.debug("Registered {} formatters", messageProcessors.size());
    }

    public AiidaMessageProcessor processorFor(String topic) throws AiidaMessageProcessorRegistryException {
        var topicSuffix = extractTopicSuffix(topic);

        if (messageProcessors.containsKey(topicSuffix)) {
            return messageProcessors.get(topicSuffix);
        }

        throw new AiidaMessageProcessorRegistryException(topic);
    }

    private String extractTopicSuffix(String topic) {
        return topic.substring(MESSAGE_VERSION_LENGTH + DELIMITER_LENGTH + PERMISSION_ID_LENGTH + DELIMITER_LENGTH);
    }
}
