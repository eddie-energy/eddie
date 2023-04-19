package at.eda.utils;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CRCTest {
    /**
     * The test values are taken from the CMRequest schema document found
     * on <a href="https://www.ebutilities.at/schemas/149">ebutilities</a> in the documents section.
     */
    @Test
    void computeCRC32_forKnownInput_returnsExpected() {
        var input = "AT999999201812312359598880000000001";
        var expected = 0x45a2dff1;

        assertEquals(expected, CRC.computeCRC32(input.getBytes()));
    }

    /**
     * The test values are taken from the CMRequest schema document found
     * on <a href="https://www.ebutilities.at/schemas/149">ebutilities</a> in the documents section.
     */
    @Test
    void computeCRC8_forKnownInput_returnsExpected() {
        var input = 0x45a2dff1;
        var expected = 0xF6;

        assertEquals(expected, CRC.computeCRC8DVBS2(ByteBuffer.allocate(Integer.BYTES).putInt(input).array()));
    }
}
