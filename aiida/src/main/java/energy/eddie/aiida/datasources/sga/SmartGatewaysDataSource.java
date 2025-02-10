package energy.eddie.aiida.datasources.sga;

import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.MqttDataSource;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
public class SmartGatewaysDataSource extends MqttDataSource {
    public SmartGatewaysDataSource() {
    }

    public SmartGatewaysDataSource(String name, boolean enabled, UUID userId, AiidaAsset asset, DataSourceType dataSourceType, String mqttServerUri, String mqttSubscribeTopic, String mqttUsername, String mqttPassword) {
        super(name, enabled, userId, asset, dataSourceType, mqttServerUri, mqttSubscribeTopic, mqttUsername, mqttPassword);
    }
}
