package energy.eddie.aiida.models.datasource;

import energy.eddie.aiida.datasources.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.datasources.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.datasources.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.datasources.simulation.SimulationDataSource;
import energy.eddie.aiida.dtos.DataSourceDto;

import java.util.UUID;

public class DataSourceFactory {
    private DataSourceFactory() {}

    public static DataSource createFromDto(
            DataSourceDto dto,
            UUID userId,
            String mqttServerUri,
            String mqttUsername,
            String mqttPassword
    ) {
        var dataSourceType = DataSourceType.fromIdentifier(dto.dataSourceType());

        return switch (dataSourceType) {
            case OESTERREICHS_ENERGIE ->
                    new OesterreichsEnergieDataSource(dto, userId, mqttServerUri, mqttUsername, mqttPassword);
            case MICRO_TELEINFO_V3 ->
                    new MicroTeleinfoV3DataSource(dto, userId, mqttServerUri, mqttUsername, mqttPassword);
            case SMART_GATEWAYS -> new SmartGatewaysDataSource(dto, userId, mqttServerUri, mqttUsername, mqttPassword);
            case SIMULATION -> new SimulationDataSource(dto, userId);
        };
    }

    public static DataSource createFromDto(DataSourceDto dto, UUID userId, DataSource currentDataSource) {
        var mqttServerUri = "";
        var mqttUsername = "";
        var mqttPassword = "";

        if (currentDataSource instanceof MqttDataSource mqttDataSource) {
            mqttServerUri = mqttDataSource.mqttServerUri();
            mqttUsername = mqttDataSource.mqttUsername();
            mqttPassword = mqttDataSource.mqttPassword();
        }

        return createFromDto(dto, userId, mqttServerUri, mqttUsername, mqttPassword);
    }
}
