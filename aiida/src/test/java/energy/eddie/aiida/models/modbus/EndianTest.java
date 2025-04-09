package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndianTest {

    @Test
    void testFromString_withValidValues() {
        assertEquals(Endian.BIG, Endian.fromString("BIG"));
        assertEquals(Endian.LITTLE, Endian.fromString("LITTLE"));
        assertEquals(Endian.UNKNOWN, Endian.fromString("UNKNOWN"));
    }

    @Test
    void testFromString_isCaseInsensitive() {
        assertEquals(Endian.BIG, Endian.fromString("big"));
        assertEquals(Endian.LITTLE, Endian.fromString("lItTlE"));
        assertEquals(Endian.UNKNOWN, Endian.fromString("UnKnOwN"));
    }

    @Test
    void testFromString_withInvalidValueReturnsUnknown() {
        assertEquals(Endian.UNKNOWN, Endian.fromString("MID"));
        assertEquals(Endian.UNKNOWN, Endian.fromString("123"));
        assertEquals(Endian.UNKNOWN, Endian.fromString("littleEndian"));
    }

    @Test
    void testFromString_withNullReturnsUnknown() {
        assertEquals(Endian.UNKNOWN, Endian.fromString(null));
    }

    @Test
    void testFromString_withEmptyStringReturnsUnknown() {
        assertEquals(Endian.UNKNOWN, Endian.fromString(""));
        assertEquals(Endian.UNKNOWN, Endian.fromString(" "));
    }
}
