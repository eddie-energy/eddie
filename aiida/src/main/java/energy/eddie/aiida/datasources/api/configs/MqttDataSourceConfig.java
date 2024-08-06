package energy.eddie.aiida.datasources.api.configs;

public interface MqttDataSourceConfig extends AiidaDataSourceConfig {
    String mqttServerUri();

    String mqttSubscribeTopic();

    String mqttUsername();

    String mqttPassword();
}
