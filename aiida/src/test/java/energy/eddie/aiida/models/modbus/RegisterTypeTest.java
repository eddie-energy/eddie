package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterTypeTest {

    @Test
    void testFromString_withValidValues() {
        assertEquals(RegisterType.HOLDING, RegisterType.fromString("HOLDING"));
        assertEquals(RegisterType.INPUT, RegisterType.fromString("input")); // case insensitive
        assertEquals(RegisterType.COIL, RegisterType.fromString("CoIl"));   // mixed case
        assertEquals(RegisterType.DISCRETE, RegisterType.fromString("discrete"));
    }

    @Test
    void testFromString_withInvalidValues() {
        assertEquals(RegisterType.UNKNOWN, RegisterType.fromString("invalid"));
        assertEquals(RegisterType.UNKNOWN, RegisterType.fromString(""));
        assertEquals(RegisterType.UNKNOWN, RegisterType.fromString("123"));
        assertEquals(RegisterType.UNKNOWN, RegisterType.fromString("HOLD ING")); // space not allowed
    }

    @Test
    void testFromString_withNullValue() {
        assertEquals(RegisterType.UNKNOWN, RegisterType.fromString(null));
    }
}
