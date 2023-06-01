package energy.eddie.regionconnector.at.eda.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CMRequestIdTest {
    /**
     * The test values are taken from the CMRequest schema document found
     * on <a href="https://www.ebutilities.at/schemas/149">ebutilities</a> in the documents section.
     */
    @Test
    void toString_withKnownInput_returnsExpectedResult() {
        var messageId = "AT999999201812312359598880000000001";
        var expected = "IWRN74PW";

        var cmRequestId = new CMRequestId(messageId);

        assertEquals(expected, cmRequestId.toString());
    }
}
