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

        assertEquals(SourceCategory.INVERTER, source.getCategory());
        assertEquals("source-1", source.getId());
        assertNotNull(source.getDatapoints());
        assertEquals(1, source.getDatapoints().size());
        ModbusDataPoint dp = source.getDatapoints().get(0);
        assertEquals("voltage", dp.getId());
        assertEquals(100, dp.getRegister());
        assertEquals(RegisterType.HOLDING, dp.getRegisterType());
        assertEquals("int16", dp.getValueType());
        assertEquals(1, dp.getLength());
        assertEquals(Endian.BIG, dp.getEndian());
        assertFalse(dp.isVirtual());
        assertEquals(Access.READ, dp.getAccess());
    }

    @Test
    void shouldSetAndGetId() {
        ModbusSource source = new ModbusSource(SourceCategory.PV, "pv-1", List.of());
        assertEquals("pv-1", source.getId());

        source.setId("pv-2");
        assertEquals("pv-2", source.getId());
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

        assertEquals(SourceCategory.BATTERY, source.getCategory());
        assertEquals(1, source.getDatapoints().size());
        assertEquals("power", source.getDatapoints().get(0).getId());
    }
}
