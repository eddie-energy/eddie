package energy.eddie.aiida.dtos.datasource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.dtos.datasource.modbus.ModbusDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.shelly.ShellyDataSourceDto;
import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DataSourceDtoTest {
    private final ObjectMapper objectMapper = new AiidaConfiguration()
            .customObjectMapper()
            .build();

    @Test
    void parseJson_givenSimulation() throws JsonProcessingException {
        // Given
        var json = "{\"name\":\"Test Source\",\"dataSourceType\":\"SIMULATION\", \"pollingInterval\": 12}";

        // When
        var dto = objectMapper.readValue(json, DataSourceDto.class);

        // Then
        assertInstanceOf(SimulationDataSourceDto.class, dto);
        assertEquals(12, ((SimulationDataSourceDto) dto).pollingInterval());
    }

    @Test
    void parseJson_givenShelly() throws JsonProcessingException {
        // Given
        var json = "{\"name\":\"Test Source\",\"dataSourceType\":\"SHELLY\"}";

        // When
        var dto = objectMapper.readValue(json, DataSourceDto.class);

        // Then
        assertInstanceOf(ShellyDataSourceDto.class, dto);
    }

    @Test
    void parseJson_givenSinapsi() throws JsonProcessingException {
        // Given
        var json = "{\"name\":\"Test Source\",\"dataSourceType\":\"SINAPSI_ALFA\", \"activationKey\": \"abc\"}";

        // When
        var dto = objectMapper.readValue(json, DataSourceDto.class);

        // Then
        assertInstanceOf(SinapsiAlfaDataSourceDto.class, dto);
        assertEquals("abc", ((SinapsiAlfaDataSourceDto) dto).activationKey());
    }

    @Test
    void parseJson_givenModbus() throws JsonProcessingException {
        // Given
        var json = "{\"name\":\"Test Source\",\"dataSourceType\":\"MODBUS\", \"modbusIp\": \"abc\"}";

        // When
        var dto = objectMapper.readValue(json, DataSourceDto.class);

        // Then
        assertInstanceOf(ModbusDataSourceDto.class, dto);
        assertEquals("abc", ((ModbusDataSourceDto) dto).modbusIp());
    }
}
