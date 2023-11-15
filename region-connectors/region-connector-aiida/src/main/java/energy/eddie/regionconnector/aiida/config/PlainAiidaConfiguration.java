package energy.eddie.regionconnector.aiida.config;

import org.apache.kafka.common.internals.Topic;

public record PlainAiidaConfiguration(
        String kafkaBoostrapServers,
        String kafkaDataTopic,
        String kafkaStatusMessagesTopic,
        String kafkaTerminationTopicPrefix
) implements AiidaConfiguration {
    public PlainAiidaConfiguration {
        Topic.validate(kafkaDataTopic);
        Topic.validate(kafkaStatusMessagesTopic);
        Topic.validate(kafkaTerminationTopicPrefix);
    }
}