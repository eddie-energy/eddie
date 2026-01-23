// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.datasource;

import energy.eddie.aiida.dtos.datasource.modbus.ModbusDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.shelly.ShellyDataSourceDto;
import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DataSourceDtoTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseJson_givenSimulation() {
        // Given
        var json = "{\"name\":\"Test Source\",\"type\":\"SIMULATION\", \"pollingInterval\": 12}";

        // When
        var dto = objectMapper.readValue(json, DataSourceDto.class);

        // Then
        assertInstanceOf(SimulationDataSourceDto.class, dto);
        assertEquals(12, ((SimulationDataSourceDto) dto).pollingInterval());
    }

    @Test
    void parseJson_givenShelly() {
        // Given
        var json = "{\"name\":\"Test Source\",\"type\":\"SHELLY\"}";

        // When
        var dto = objectMapper.readValue(json, DataSourceDto.class);

        // Then
        assertInstanceOf(ShellyDataSourceDto.class, dto);
    }

    @Test
    void parseJson_givenSinapsi() {
        // Given
        var json = "{\"name\":\"Test Source\",\"type\":\"SINAPSI_ALFA\", \"activationKey\": \"abc\"}";

        // When
        var dto = objectMapper.readValue(json, DataSourceDto.class);

        // Then
        assertInstanceOf(SinapsiAlfaDataSourceDto.class, dto);
        assertEquals("abc", ((SinapsiAlfaDataSourceDto) dto).activationKey());
    }

    @Test
    void parseJson_givenModbus() {
        // Given
        var ipAddress = "127.0.0.1";
        var json = "{\"name\":\"Test Source\",\"type\":\"MODBUS\", \"ipAddress\": \"" + ipAddress + "\"}";

        // When
        var dto = objectMapper.readValue(json, DataSourceDto.class);

        // Then
        assertInstanceOf(ModbusDataSourceDto.class, dto);
        assertEquals(ipAddress, ((ModbusDataSourceDto) dto).ipAddress());
    }
}
