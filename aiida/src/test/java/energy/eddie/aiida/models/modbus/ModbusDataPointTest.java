// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModbusDataPointTest {

    @Test
    void testLength_whenLengthSetExplicitly() {
        ModbusDataPoint dp = new ModbusDataPoint(
                "voltage", 100, RegisterType.HOLDING, 2, "int32",
                Endian.BIG, false, null, null, null, Access.READ
        );

        assertThat(dp.length()).isEqualTo(2);
    }

    @Test
    void testLength_forCoilAndDiscreteReturns1() {
        ModbusDataPoint coilDp = new ModbusDataPoint(
                "coil", 1, RegisterType.COIL, 0, "any", null, false, null, null, null, Access.READ
        );
        assertThat(coilDp.length()).isEqualTo(1);

        ModbusDataPoint discreteDp = new ModbusDataPoint(
                "discrete", 2, RegisterType.DISCRETE, 0, "any", null, false, null, null, null, Access.READ
        );
        assertThat(discreteDp.length()).isEqualTo(1);
    }

    @Test
    void testLength_forKnownValueTypes() {
        assertThat(createDataPoint("dp16", "int16").length()).isEqualTo(1);
        assertThat(createDataPoint("dp32", "int32").length()).isEqualTo(2);
        assertThat(createDataPoint("dpf32", "float32").length()).isEqualTo(2);
    }

    @Test
    void testLength_forUnknownValueType_defaultsTo1() {
        ModbusDataPoint dp = createDataPoint("unknown", "crazytype");
        assertThat(dp.length()).isEqualTo(1);
    }

    @Test
    void testLength_whenMissingType_returns0() {
        ModbusDataPoint dp = new ModbusDataPoint(
                "broken", 1, null, 0, null, null, false, null, null, null, Access.READ
        );
        assertThat(dp.length()).isZero();
    }

    @Test
    void testStringLengthWarningDefaultsTo1() {
        ModbusDataPoint dp = createDataPoint("stringdp", "string");
        assertThat(dp.length()).isEqualTo(1);
    }

    @Test
    void testGetters() {
        ModbusDataPoint dp = new ModbusDataPoint(
                "dp", 123, RegisterType.HOLDING, 3, "int16",
                Endian.BIG, true, List.of("new"), "x*2", Map.of("on", "1"), Access.WRITE
        );

        assertThat(dp.id()).isEqualTo("dp");
        assertThat(dp.register()).isEqualTo(123);
        assertThat(dp.length()).isEqualTo(3);
        assertThat(dp.registerType()).isEqualTo(RegisterType.HOLDING);
        assertThat(dp.valueType()).isEqualTo("int16");
        assertThat(dp.endian()).isEqualTo(Endian.BIG);
        assertThat(dp.virtual()).isTrue();
        assertThat(dp.sources()).containsExactly("new");
        assertThat(dp.transform()).isEqualTo("x*2");
        assertThat(dp.translations()).containsEntry("on", "1");
        assertThat(dp.access()).isEqualTo(Access.WRITE);
    }

    private ModbusDataPoint createDataPoint(String id, String valueType) {
        return new ModbusDataPoint(
                id, 10, RegisterType.HOLDING, 0, valueType,
                Endian.BIG, false, null, null, null, Access.READ
        );
    }
}
