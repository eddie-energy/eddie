package energy.eddie.aiida.models.datasource;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceModbusDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.modbus.ModbusDataSource;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.models.datasource.mqtt.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyDataSource;
import energy.eddie.aiida.models.datasource.simulation.SimulationDataSource;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceTest {
    private static final String COUNTRY_CODE = "AT";
    private static final UUID VENDOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODEL_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DEVICE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final DataSourceMqttDto MQTT_DTO = new DataSourceMqttDto("tcp://localhost:1883",
                                                                            "tcp://localhost:1883",
                                                                            "aiida/test",
                                                                            "user",
                                                                            "pw");
    private static final UUID ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");

    private static final DataSourceModbusDto MODBUS_DTO = new DataSourceModbusDto(
            "192.168.1.100", VENDOR_ID, MODEL_ID, DEVICE_ID);

    private static final DataSourceModbusDto MODBUS_DTO_NO_IP = new DataSourceModbusDto(
            null, VENDOR_ID, MODEL_ID, DEVICE_ID);

    DataSourceDto createNewDataSourceDto(DataSourceType type) {
        return new DataSourceDto(ID, type, AiidaAsset.SUBMETER, "test", COUNTRY_CODE, true, DataSourceIcon.METER, 1, null, null);
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
    void givenShelly_returnsDataSource() {
        // Given
        var dto = createNewDataSourceDto(DataSourceType.SHELLY);

        // When
        var dataSource = DataSource.createFromDto(dto, ID, MQTT_DTO);

        // Then
        assertInstanceOf(ShellyDataSource.class, dataSource);
    }

    @Test
    void givenInbound_returnsDataSource() {
        // Given
        var dto = createNewDataSourceDto(DataSourceType.INBOUND);

        // When
        var dataSource = DataSource.createFromDto(dto, ID, MQTT_DTO);

        // Then
        assertInstanceOf(InboundDataSource.class, dataSource);
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

    @Test
    void givenModbus_returnsDataSource() {
        var dto = createNewDataSourceDto(DataSourceType.MODBUS);
        var dataSource = DataSource.createFromDto(dto, ID, MODBUS_DTO);
        assertInstanceOf(ModbusDataSource.class, dataSource);
    }

    @Test
    void throwsOnInvalidSmartMeterAdapterSettings() {
        var dataSourceType = DataSourceType.SMART_METER_ADAPTER;
        var dto = createNewDataSourceDto(dataSourceType);
        Exception exception = assertThrows(IllegalStateException.class, () ->
                DataSource.createFromDto(dto, ID, MODBUS_DTO));
        assertEquals("Expected MQTT settings for %s datasource".formatted(dataSourceType), exception.getMessage());
    }

    @Test
    void throwsOnInvalidModbusSettings() {
        var dataSourceType = DataSourceType.MODBUS;
        var dto = createNewDataSourceDto(dataSourceType);
        Exception exception = assertThrows(IllegalStateException.class, () ->
                DataSource.createFromDto(dto, ID, MQTT_DTO));
        assertEquals("Expected MODBUS settings for %s datasource".formatted(dataSourceType), exception.getMessage());
    }

    @Test
    void mergeWithDto_updatesModbusDataSource() {
        var dto = createNewDataSourceDto(DataSourceType.MODBUS);
        var original = (ModbusDataSource) DataSource.createFromDto(dto, ID, MODBUS_DTO);

        var updatedDto = new DataSourceDto(ID,
                                           DataSourceType.MODBUS,
                                           AiidaAsset.SUBMETER,
                                           "test-updated",
                                           COUNTRY_CODE,
                                           true,
                                           DataSourceIcon.METER,
                                           1,
                                           null,
                                           new DataSourceModbusDto("192.168.1.200", VENDOR_ID, MODEL_ID, DEVICE_ID));

        DataSource merged = original.mergeWithDto(updatedDto, ID);
        assertInstanceOf(ModbusDataSource.class, merged);
        assertEquals("192.168.1.200", ((ModbusDataSource) merged).modbusIp());
    }

    @Test
    void mergeWithDto_throwsOnMissingModbusIp() {
        var dto = createNewDataSourceDto(DataSourceType.MODBUS);
        var original = (ModbusDataSource) DataSource.createFromDto(dto, ID, MODBUS_DTO_NO_IP);


        var updatedDto = new DataSourceDto(ID,
                                           DataSourceType.MODBUS,
                                           AiidaAsset.SUBMETER,
                                           "test",
                                           COUNTRY_CODE,
                                           true,
                                           DataSourceIcon.METER,
                                           1,
                                           null,
                                           new DataSourceModbusDto(null, VENDOR_ID, MODEL_ID, DEVICE_ID));

        assertThrows(IllegalArgumentException.class, () -> original.mergeWithDto(updatedDto, ID));
    }

    @Test
    void mergeWithDto_dontThrowsOnMissingModbusIp() {
        var dto = createNewDataSourceDto(DataSourceType.MODBUS);
        var original = (ModbusDataSource) DataSource.createFromDto(dto, ID, MODBUS_DTO);


        var updatedDto = new DataSourceDto(ID,
                                           DataSourceType.MODBUS,
                                           AiidaAsset.SUBMETER,
                                           "test",
                                           COUNTRY_CODE,
                                           true,
                                           DataSourceIcon.METER,
                                           1,
                                           null,
                                           new DataSourceModbusDto(null, VENDOR_ID, MODEL_ID, DEVICE_ID));

        // No exception should be thrown because original has a valid IP
        DataSource merged = original.mergeWithDto(updatedDto, ID);
        assertInstanceOf(ModbusDataSource.class, merged);
        assertEquals("192.168.1.100", ((ModbusDataSource) merged).modbusIp());
    }

    @Test
    void mergeWithDto_returnsMqttDataSource() {
        var dto = createNewDataSourceDto(DataSourceType.SMART_GATEWAYS_ADAPTER);
        var original = DataSource.createFromDto(dto, ID, MQTT_DTO);
        var merged = original.mergeWithDto(dto, ID);
        assertInstanceOf(SmartGatewaysDataSource.class, merged);
    }

    @Test
    void testToDtoAndGetters() {
        var dto = createNewDataSourceDto(DataSourceType.SMART_GATEWAYS_ADAPTER);
        var source = DataSource.createFromDto(dto, ID, MQTT_DTO);
        var converted = source.toDto();

        assertEquals(dto.id(), source.id());
        assertEquals(dto.asset(), source.asset());
        assertEquals(dto.name(), source.name());
        assertEquals(dto.enabled(), source.enabled());
        assertEquals(dto.dataSourceType(), source.dataSourceType());
        assertEquals(dto.dataSourceType(), converted.dataSourceType());
    }
}
