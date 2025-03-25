package energy.eddie.aiida.models.datasource;

import energy.eddie.aiida.models.datasource.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.models.datasource.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.models.datasource.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.simulation.SimulationDataSource;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DataSourceTest {
    private static final DataSourceMqttDto MQTT_DTO = new DataSourceMqttDto("tcp://localhost:1883",
                                                                            "tcp://localhost:1883",
                                                                            "aiida/test",
                                                                            "user",
                                                                            "pw");
    private static final UUID ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");

    DataSourceDto createNewDataSourceDto(DataSourceType type) {
        return new DataSourceDto(ID, type.identifier(), AiidaAsset.SUBMETER.asset(), "test", true, "", 1, null);
    }

    @Test
    void givenSmartMeterAdapter_returnsDataSource() {
        // Given
        var dto = createNewDataSourceDto(DataSourceType.SMART_METER_ADAPTER);

        // When
        var dataSource = DataSource.createFromDto(dto, ID, MQTT_DTO);

        // Then
        assertInstanceOf(OesterreichsEnergieDataSource.class, dataSource);
    }

    @Test
    void givenMicroTeleinfo_returnsDataSource() {
        // Given
        var dto = createNewDataSourceDto(DataSourceType.MICRO_TELEINFO);

        // When
        var dataSource = DataSource.createFromDto(dto, ID, MQTT_DTO);

        // Then
        assertInstanceOf(MicroTeleinfoV3DataSource.class, dataSource);
    }

    @Test
    void givenSmartGatewaysAdapter_returnsDataSource() {
        // Given
        var dto = createNewDataSourceDto(DataSourceType.SMART_GATEWAYS_ADAPTER);

        // When
        var dataSource = DataSource.createFromDto(dto, ID, MQTT_DTO);

        // Then
        assertInstanceOf(SmartGatewaysDataSource.class, dataSource);
    }

    @Test
    void givenSimulation_returnsDataSource() {
        // Given
        var dto = createNewDataSourceDto(DataSourceType.SIMULATION);

        // When
        var dataSource = DataSource.createFromDto(dto, ID, MQTT_DTO);

        // Then
        assertInstanceOf(SimulationDataSource.class, dataSource);
    }
}
