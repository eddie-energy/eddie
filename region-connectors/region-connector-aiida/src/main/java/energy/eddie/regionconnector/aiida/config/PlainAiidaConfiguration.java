package energy.eddie.regionconnector.aiida.config;

import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.internals.Topic;

public record PlainAiidaConfiguration(
        String kafkaBoostrapServers,
        String kafkaDataTopic,
        String kafkaStatusMessagesTopic,
        String kafkaTerminationTopicPrefix,
        String customerId
) implements AiidaConfiguration {

    /**
     * Will validate if the passed topic names or prefixes contain only characters that may be used in a Kafka topic name.
     *
     * @param kafkaBoostrapServers        List of Kafka bootstrap servers.
     * @param kafkaDataTopic              Kafka topic on which all AIIDA instances will publish their data records.
     * @param kafkaStatusMessagesTopic    Kafka topic on which all AIIDA instances will publish their connection status messages.
     * @param kafkaTerminationTopicPrefix Prefix that should be used for Kafka topics on which the termination request
     *                                    for a specific AIIDA instances will be published by this region connector.
     * @throws InvalidTopicException If any passed topic name/prefix contains not valid characters.
     */
    public PlainAiidaConfiguration {
        Topic.validate(kafkaDataTopic);
        Topic.validate(kafkaStatusMessagesTopic);
        Topic.validate(kafkaTerminationTopicPrefix);
    }
}