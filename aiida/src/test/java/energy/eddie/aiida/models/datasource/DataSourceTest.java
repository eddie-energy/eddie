package energy.eddie.aiida.models.datasource;

import energy.eddie.aiida.dtos.datasource.modbus.ModbusDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.at.OesterreichsEnergieDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.fr.MicroTeleinfoV3DataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.inbound.InboundDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.sga.SmartGatewaysDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.shelly.ShellyDataSourceDto;
import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import energy.eddie.aiida.models.datasource.modbus.ModbusDataSource;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.models.datasource.mqtt.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyDataSource;
import energy.eddie.aiida.models.datasource.simulation.SimulationDataSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DataSourceTest {
    private static final UUID ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");

    @Test
    void givenSmartMeterAdapter_returnsDataSource() {
        // Given
        var dto = mock(OesterreichsEnergieDataSourceDto.class);

        // When
        var dataSource = DataSource.createFromDto(dto, ID);

        // Then
        assertInstanceOf(OesterreichsEnergieDataSource.class, dataSource);
    }

    @Test
    void givenMicroTeleinfo_returnsDataSource() {
        // Given
        var dto = mock(MicroTeleinfoV3DataSourceDto.class);

        // When
        var dataSource = DataSource.createFromDto(dto, ID);

        // Then
        assertInstanceOf(MicroTeleinfoV3DataSource.class, dataSource);
    }

    @Test
    void givenSinapsiAlfa_returnsDataSource() {
        // Given
        var dto = mock(SinapsiAlfaDataSourceDto.class);

        // When
        var dataSource = DataSource.createFromDto(dto, ID);

        // Then
        assertInstanceOf(SinapsiAlfaDataSource.class, dataSource);
    }

    @Test
    void givenSmartGatewaysAdapter_returnsDataSource() {
        // Given
        var dto = mock(SmartGatewaysDataSourceDto.class);

        // When
        var dataSource = DataSource.createFromDto(dto, ID);

        // Then
        assertInstanceOf(SmartGatewaysDataSource.class, dataSource);
    }

    @Test
    void givenShelly_returnsDataSource() {
        // Given
        var dto = mock(ShellyDataSourceDto.class);

        // When
        var dataSource = DataSource.createFromDto(dto, ID);

        // Then
        assertInstanceOf(ShellyDataSource.class, dataSource);
    }

    @Test
    void givenInbound_throwsIllegalArgumentException() {
        // Given
        var dto = mock(InboundDataSourceDto.class);

        // When, Then
        assertThrows(IllegalArgumentException.class, () -> DataSource.createFromDto(dto, ID));
    }

    @Test
    void givenSimulation_returnsDataSource() {
        // Given
        var dto = mock(SimulationDataSourceDto.class);

        // When
        var dataSource = DataSource.createFromDto(dto, ID);

        // Then
        assertInstanceOf(SimulationDataSource.class, dataSource);
    }

    @Test
    void givenModbus_returnsDataSource() {
        // Given
        var dto = mock(ModbusDataSourceDto.class);

        // When
        var dataSource = DataSource.createFromDto(dto, ID);

        // Then
        assertInstanceOf(ModbusDataSource.class, dataSource);
    }
}
