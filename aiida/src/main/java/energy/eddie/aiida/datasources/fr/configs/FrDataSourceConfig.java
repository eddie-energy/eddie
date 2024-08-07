package energy.eddie.aiida.datasources.fr.configs;

import energy.eddie.aiida.datasources.api.configs.MqttDataSourceConfig;

public record FrDataSourceConfig(
        boolean enabled,
        String id,
        String mqttServerUri,
        String mqttSubscribeTopic,
        String mqttUsername,
        String mqttPassword,
        String meteringId
) implements MqttDataSourceConfig {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FrDataSourceConfig that)) return false;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}