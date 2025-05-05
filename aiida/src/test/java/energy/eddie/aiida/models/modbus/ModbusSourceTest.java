package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModbusSourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeFromJsonCorrectly() throws Exception {
        String json = """
                {
                    "category": "INVERTER",
                    "id": "source-1",
                    "datapoints": [
                        {
                            "id": "voltage",
                            "register": 100,
                            "registerType": "HOLDING",
                            "length": 1,
                            "valueType": "int16",
                            "endian": "BIG",
                            "virtual": false,
                            "source": [],
                            "transform": null,
                            "translations": {},
                            "access": "READ"
                        }
                    ]
                }
                """;

        ModbusSource source = objectMapper.readValue(json, ModbusSource.class);

        assertEquals(SourceCategory.INVERTER, source.category());
        assertEquals("source-1", source.id());
        assertNotNull(source.dataPoints());
        assertEquals(1, source.dataPoints().size());

        ModbusDataPoint dp = source.dataPoints().getFirst();
        assertEquals("voltage", dp.id());
        assertEquals(100, dp.register());
        assertEquals(RegisterType.HOLDING, dp.registerType());
        assertEquals("int16", dp.valueType());
        assertEquals(1, dp.getLength()); // still method
        assertEquals(Endian.BIG, dp.endian());
        assertFalse(dp.virtual());
        assertEquals(Access.READ, dp.access());
    }

    @Test
    void shouldReturnCorrectCategoryAndDatapoints() {
        ModbusDataPoint dp = new ModbusDataPoint(
                "power",
                200,
                RegisterType.INPUT,
                2,
                "float32",
                Endian.LITTLE,
                false,
                List.of(),
                null,
                null,
                Access.READWRITE
        );

        ModbusSource source = new ModbusSource(SourceCategory.BATTERY, "battery-1", List.of(dp));

        assertEquals(SourceCategory.BATTERY, source.category());
        assertEquals("battery-1", source.id());
        assertEquals(1, source.dataPoints().size());
        assertEquals("power", source.dataPoints().getFirst().id());
    }
}
