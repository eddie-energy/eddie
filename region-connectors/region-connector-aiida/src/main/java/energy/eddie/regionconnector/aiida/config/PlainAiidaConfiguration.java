package energy.eddie.regionconnector.aiida.config;

public record PlainAiidaConfiguration(
        String kafkaBoostrapServers,
        String kafkaDataTopic,
        String kafkaStatusMessagesTopic,
        String kafkaTerminationTopicPrefix
) implements AiidaConfiguration {
}