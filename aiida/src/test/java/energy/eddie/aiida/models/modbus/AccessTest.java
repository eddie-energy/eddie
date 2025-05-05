package energy.eddie.aiida.models.modbus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessTest {

    @Test
    void testFromStringValidValues() {
        assertEquals(Access.READ, Access.fromString("read"));
        assertEquals(Access.WRITE, Access.fromString("write"));
        assertEquals(Access.READWRITE, Access.fromString("readwrite"));
        assertEquals(Access.READ, Access.fromString("READ")); // case-insensitive
    }

    @Test
    void testFromStringInvalidValueReturnsUnknown() {
        assertEquals(Access.UNKNOWN, Access.fromString("invalid"));
        assertEquals(Access.UNKNOWN, Access.fromString(null));
        assertEquals(Access.UNKNOWN, Access.fromString(""));
    }

    @Test
    void testCanRead() {
        assertTrue(Access.READ.canRead());
        assertTrue(Access.READWRITE.canRead());
        assertFalse(Access.WRITE.canRead());
        assertFalse(Access.UNKNOWN.canRead());
    }

    @Test
    void testCanWrite() {
        assertTrue(Access.WRITE.canWrite());
        assertTrue(Access.READWRITE.canWrite());
        assertFalse(Access.READ.canWrite());
        assertFalse(Access.UNKNOWN.canWrite());
    }
}
