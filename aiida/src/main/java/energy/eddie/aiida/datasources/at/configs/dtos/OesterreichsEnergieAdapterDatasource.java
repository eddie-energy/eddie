package energy.eddie.aiida.datasources.at.configs.dtos;

import energy.eddie.aiida.datasources.api.configs.MqttDataSourceConfig;

public record OesterreichsEnergieAdapterDatasource(
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
        if (!(o instanceof OesterreichsEnergieAdapterDatasource that)) return false;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
