package energy.eddie.regionconnector.aiida.config;

public interface AiidaConfiguration {
    String PREFIX = "region-connector.aiida.";
    String KAFKA_BOOTSTRAP_SERVERS = PREFIX + "kafka.bootstrap-servers";
    String KAFKA_DATA_TOPIC = PREFIX + "kafka.data-topic";
    String KAFKA_STATUS_MESSAGES_TOPIC = PREFIX + "kafka.status-messages-topic";
    String KAFKA_TERMINATION_TOPIC_PREFIX = PREFIX + "kafka.termination-topic-prefix";
    String KAFKA_GROUP_ID = PREFIX + "kafka.group-id";

    /**
     * Returns the list of Kafka brokers to which this region connector will connect to, and which will be added
     * to any permission requests, i.e. where AIIDA instances will send their data.
     *
     * @return List of Kafka bootstrap servers.
     */
    String kafkaBoostrapServers();

    /**
     * Kafka topic on which all AIIDA instances will publish their data records.
     *
     * @return Kafka topic name.
     */
    String kafkaDataTopic();

    /**
     * Kafka topic on which all AIIDA instances will publish their connection status messages.
     *
     * @return Kafka topic name.
     */
    String kafkaStatusMessagesTopic();

    /**
     * Prefix that should be used for Kafka topics on which the termination request for a specific
     * AIIDA instances will be published by this region connector.
     *
     * @return Kafka topic name prefix.
     */
    String kafkaTerminationTopicPrefix();
}
