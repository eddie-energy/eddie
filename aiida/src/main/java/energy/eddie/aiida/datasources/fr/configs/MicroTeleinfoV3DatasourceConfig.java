package energy.eddie.aiida.datasources.fr.configs;

import energy.eddie.aiida.datasources.api.configs.MqttDataSourceConfig;

public interface MicroTeleinfoV3DatasourceConfig extends MqttDataSourceConfig {
    String meteringId();
}
