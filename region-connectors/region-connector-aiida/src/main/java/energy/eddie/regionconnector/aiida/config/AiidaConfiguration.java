package energy.eddie.regionconnector.aiida.config;

public interface AiidaConfiguration {
    String PREFIX = "region-connector.aiida.";
    String KAFKA_BOOTSTRAP_SERVERS = PREFIX + "kafka.bootstrap-servers";
    String KAFKA_DATA_TOPIC = PREFIX + "kafka.data-topic";
    String KAFKA_STATUS_MESSAGES_TOPIC = PREFIX + "kafka.status-messages-topic";
    String KAFKA_TERMINATION_TOPIC_PREFIX = PREFIX + "kafka.termination-topic-prefix";

    String kafkaBoostrapServers();

    String kafkaDataTopic();

    String kafkaStatusMessagesTopic();

    String kafkaTerminationTopicPrefix();
}
