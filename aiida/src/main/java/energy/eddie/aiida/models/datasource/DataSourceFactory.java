package energy.eddie.aiida.models.datasource;

import energy.eddie.aiida.datasources.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.datasources.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.datasources.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.datasources.simulation.SimulationDataSource;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;

import java.util.UUID;

public class DataSourceFactory {
    private DataSourceFactory() {}

    public static DataSource createFromDto(DataSourceDto dto, UUID userId, DataSourceMqttDto dataSourceMqttDto) {
        var dataSourceType = DataSourceType.fromIdentifier(dto.dataSourceType());

        return switch (dataSourceType) {
            case SMART_METER_ADAPTER -> new OesterreichsEnergieDataSource(dto, userId, dataSourceMqttDto);
            case MICRO_TELEINFO -> new MicroTeleinfoV3DataSource(dto, userId, dataSourceMqttDto);
            case SMART_GATEWAYS_ADAPTER -> new SmartGatewaysDataSource(dto, userId, dataSourceMqttDto);
            case SIMULATION -> new SimulationDataSource(dto, userId);
        };
    }

    public static DataSource createFromDto(DataSourceDto dto, UUID userId, DataSource currentDataSource) {
        var mqttSettingsDto = new DataSourceMqttDto();

        if (currentDataSource instanceof MqttDataSource mqttDataSource) {
            mqttSettingsDto = new DataSourceMqttDto(
                    mqttDataSource.mqttServerUri(),
                    mqttDataSource.mqttSubscribeTopic(),
                    mqttDataSource.mqttUsername(),
                    mqttDataSource.mqttPassword()
            );
        }

        return createFromDto(dto, userId, mqttSettingsDto);
    }
}
