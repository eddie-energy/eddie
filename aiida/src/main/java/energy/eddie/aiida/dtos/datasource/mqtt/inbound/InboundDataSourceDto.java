package energy.eddie.aiida.dtos.datasource.mqtt.inbound;

import energy.eddie.aiida.dtos.datasource.mqtt.MqttDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceIcon;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;

import java.util.UUID;

public class InboundDataSourceDto extends MqttDataSourceDto {
    public InboundDataSourceDto(
            AiidaAsset asset,
            UUID permissionId,
            MqttStreamingConfig mqttStreamingConfig
    ) {
        this.dataSourceType = DataSourceType.INBOUND;
        this.name = permissionId.toString();
        this.asset = asset;
        this.enabled = true;
        this.icon = DataSourceIcon.ELECTRICITY;
        this.internalHost = mqttStreamingConfig.serverUri();
        this.externalHost = mqttStreamingConfig.serverUri();
        this.subscribeTopic = mqttStreamingConfig.dataTopic();
        this.username = mqttStreamingConfig.username();
        this.password = mqttStreamingConfig.password();
    }
}
