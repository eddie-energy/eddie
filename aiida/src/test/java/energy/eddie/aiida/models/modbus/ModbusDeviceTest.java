package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModbusDeviceTest {

    @Test
    void testConstructorAndGetters() {
        IntervalConfig intervalConfig = new IntervalConfig(1000L, 500L);
        Intervals intervals = new Intervals(intervalConfig);

        ModbusDataPoint dp = new ModbusDataPoint(
                "power", 100, RegisterType.HOLDING, 2,
                "int32", Endian.BIG, false, List.of(),
                null, Collections.emptyMap(), Access.READ
        );

        ModbusSource source = new ModbusSource(SourceCategory.INVERTER, "source1", List.of(dp));
        List<ModbusSource> sources = Collections.singletonList(source);

        ModbusDevice device = new ModbusDevice("dev1", "Main Device", 502, 1, intervals, sources);

        assertEquals("dev1", device.getId());
        assertEquals("Main Device", device.getName());
        assertEquals(502, device.getPort());
        assertEquals(1, device.getUnitId());
        assertSame(intervals, device.getIntervals());
        assertSame(sources, device.getSources());
        assertEquals(1000L, device.getIntervals().read().defaultInterval());
        assertEquals(500L, device.getIntervals().read().minInterval());
        assertEquals("source1", device.getSources().get(0).getId());
        assertEquals(SourceCategory.INVERTER, device.getSources().get(0).getCategory());
    }

    @Test
    void testSetters() {
        IntervalConfig intervalConfig = new IntervalConfig(1000L, 500L);
        Intervals intervals = new Intervals(intervalConfig);
        ModbusDevice device = new ModbusDevice("dev1", "Main Device", 502, 1, intervals, Collections.emptyList());

        device.setId("dev2");
        device.setName("Updated Device");

        assertEquals("dev2", device.getId());
        assertEquals("Updated Device", device.getName());
    }

    @Test
    void testImmutabilityOfFinalFields() {
        IntervalConfig config = new IntervalConfig(2000L, 1000L);
        Intervals intervals = new Intervals(config);
        List<ModbusSource> sources = Collections.emptyList();
        ModbusDevice device = new ModbusDevice("id", "name", 1234, 2, intervals, sources);

        // These final fields should remain unchanged
        assertEquals(1234, device.getPort());
        assertEquals(2, device.getUnitId());
        assertSame(intervals, device.getIntervals());
        assertSame(sources, device.getSources());
    }
}
