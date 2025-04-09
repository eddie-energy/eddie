package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModbusDataPointTest {

    @Test
    void testGetLength_whenLengthSetExplicitly() {
        ModbusDataPoint dp = new ModbusDataPoint(
                "voltage", 100, RegisterType.HOLDING, 2, "int32",
                Endian.BIG, false, null, null, null, Access.READ
        );

        assertThat(dp.getLength()).isEqualTo(2);
    }

    @Test
    void testGetLength_forCoilAndDiscreteReturns1() {
        ModbusDataPoint coilDp = new ModbusDataPoint(
                "coil", 1, RegisterType.COIL, 0, "any", null, false, null, null, null, Access.READ
        );
        assertThat(coilDp.getLength()).isEqualTo(1);

        ModbusDataPoint discreteDp = new ModbusDataPoint(
                "discrete", 2, RegisterType.DISCRETE, 0, "any", null, false, null, null, null, Access.READ
        );
        assertThat(discreteDp.getLength()).isEqualTo(1);
    }

    @Test
    void testGetLength_forKnownValueTypes() {
        assertThat(createDataPoint("dp16", "int16").getLength()).isEqualTo(1);
        assertThat(createDataPoint("dp32", "int32").getLength()).isEqualTo(2);
        assertThat(createDataPoint("dpf32", "float32").getLength()).isEqualTo(2);
    }

    @Test
    void testGetLength_forUnknownValueType_defaultsTo1() {
        ModbusDataPoint dp = createDataPoint("unknown", "crazytype");
        assertThat(dp.getLength()).isEqualTo(1);
    }

    @Test
    void testGetLength_whenMissingType_returns0() {
        ModbusDataPoint dp = new ModbusDataPoint(
                "broken", 1, null, 0, null, null, false, null, null, null, Access.READ
        );
        assertThat(dp.getLength()).isZero();
    }

    @Test
    void testStringLengthWarningDefaultsTo1() {
        ModbusDataPoint dp = createDataPoint("stringdp", "string");
        assertThat(dp.getLength()).isEqualTo(1);
    }

    @Test
    void testSettersAndGetters() {
        ModbusDataPoint dp = new ModbusDataPoint(
                "dp", 123, RegisterType.HOLDING, 0, "int16",
                Endian.LITTLE, true, List.of("a", "b"), "x*2", Map.of("on", "1"), Access.WRITE
        );

        dp.setId("updatedId");
        dp.setLength(3);
        dp.setEndian(Endian.BIG);
        dp.setSource(List.of("new"));

        assertThat(dp.getId()).isEqualTo("updatedId");
        assertThat(dp.getLength()).isEqualTo(3);
        assertThat(dp.getRegister()).isEqualTo(123);
        assertThat(dp.getRegisterType()).isEqualTo(RegisterType.HOLDING);
        assertThat(dp.getValueType()).isEqualTo("int16");
        assertThat(dp.getEndian()).isEqualTo(Endian.BIG);
        assertThat(dp.isVirtual()).isTrue();
        assertThat(dp.getSource()).containsExactly("new");
        assertThat(dp.getTransform()).isEqualTo("x*2");
        assertThat(dp.getTranslations()).containsEntry("on", "1");
        assertThat(dp.getAccess()).isEqualTo(Access.WRITE);
    }

    private ModbusDataPoint createDataPoint(String id, String valueType) {
        return new ModbusDataPoint(
                id, 10, RegisterType.HOLDING, 0, valueType,
                Endian.BIG, false, null, null, null, Access.READ
        );
    }
}
