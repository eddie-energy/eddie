package energy.eddie.regionconnector.shared.cim.v0_82;

import energy.eddie.cim.v0_82.pmd.CodingSchemeTypeList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CimUtilsTest {
    @Test
    void getCodingScheme_returnsValidSchemePmd() {
        // Given
        var cc = "AT";

        // When
        var res = CimUtils.getCodingSchemePmd(cc);

        // Then
        assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, res);
    }

    @Test
    void getCodingScheme_returnsNullOnInvalidSchemePmd() {
        // Given
        var cc = "UNKNOWN";

        // When
        var res = CimUtils.getCodingSchemePmd(cc);

        // Then
        assertNull(res);
    }
}