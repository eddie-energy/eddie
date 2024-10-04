package energy.eddie.aiida.datasources.fr.configs.dtos;

import energy.eddie.aiida.datasources.fr.configs.MicroTeleinfoV3DatasourceConfig;

public record MicroTeleinfoV3Datasource(
        boolean enabled,
        String id,
        String mqttServerUri,
        String mqttSubscribeTopic,
        String mqttUsername,
        String mqttPassword,
        String meteringId
) implements MicroTeleinfoV3DatasourceConfig {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MicroTeleinfoV3Datasource that)) return false;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}