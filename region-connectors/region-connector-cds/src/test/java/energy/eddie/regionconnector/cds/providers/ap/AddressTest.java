package energy.eddie.regionconnector.cds.providers.ap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressTest {

    @Test
    void testParse_parsesSingleLine() {
        // Given
        var input = "123 Main St Stair A Floor 2 Door 4B Apt 9, Springfield, IL 62704";

        // When
        var res = Address.parse(input);

        // Then
        assertNotNull(res);
        assertAll(
                () -> assertEquals("123", res.houseNumber()),
                () -> assertEquals("Main St", res.street()),
                () -> assertEquals("Springfield", res.city()),
                () -> assertEquals("IL", res.state()),
                () -> assertEquals("62704", res.zip()),
                () -> assertEquals("A", res.staircase()),
                () -> assertEquals("2", res.floor()),
                () -> assertEquals("4B", res.door()),
                () -> assertEquals("Apt 9", res.suffix())
        );
    }

    @Test
    void testParse_parsesMultiLineAddress() {
        // Given
        var input = """
                456 Elm Blvd Stair B Floor 5 Door 12C Suite 22
                Metropolis, NY 10001
                """;

        // When
        var res = Address.parse(input);

        // Then
        assertNotNull(res);
        assertAll(
                () -> assertEquals("456", res.houseNumber()),
                () -> assertEquals("Elm Blvd", res.street()),
                () -> assertEquals("Metropolis", res.city()),
                () -> assertEquals("NY", res.state()),
                () -> assertEquals("10001", res.zip()),
                () -> assertEquals("B", res.staircase()),
                () -> assertEquals("5", res.floor()),
                () -> assertEquals("12C", res.door()),
                () -> assertEquals("Suite 22", res.suffix())
        );
    }
}