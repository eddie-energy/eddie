// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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

        assertEquals("dev1", device.id());
        assertEquals("Main Device", device.name());
        assertEquals(502, device.port());
        assertEquals(1, device.unitId());
        assertSame(intervals, device.intervals());
        assertSame(sources, device.sources());
        assertEquals(1000L, device.intervals().read().defaultInterval());
        assertEquals(500L, device.intervals().read().minInterval());
        assertEquals("source1", device.sources().getFirst().id());
        assertEquals(SourceCategory.INVERTER, device.sources().getFirst().category());
    }

    @Test
    void testImmutabilityOfFinalFields() {
        IntervalConfig config = new IntervalConfig(2000L, 1000L);
        Intervals intervals = new Intervals(config);
        List<ModbusSource> sources = Collections.emptyList();
        ModbusDevice device = new ModbusDevice("id", "name", 1234, 2, intervals, sources);

        // These final fields should remain unchanged
        assertEquals(1234, device.port());
        assertEquals(2, device.unitId());
        assertSame(intervals, device.intervals());
        assertSame(sources, device.sources());
    }
}
