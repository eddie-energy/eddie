package energy.eddie.aiida.datasources.sga.configs.dtos;

import energy.eddie.aiida.datasources.api.configs.MqttDataSourceConfig;

public record SmartGatewaysAdapterDatasource(
        boolean enabled,
        String id,
        String mqttServerUri,
        String mqttSubscribeTopic,
        String mqttUsername,
        String mqttPassword
) implements MqttDataSourceConfig {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SmartGatewaysAdapterDatasource that)) return false;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}