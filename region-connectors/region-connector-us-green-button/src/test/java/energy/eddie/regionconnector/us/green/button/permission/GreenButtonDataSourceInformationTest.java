package energy.eddie.regionconnector.us.green.button.permission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreenButtonDataSourceInformationTest {
    @Test
    void testHashCode() {
        // Given, When, Then
        assertEquals(new GreenButtonDataSourceInformation().hashCode(),
                     new GreenButtonDataSourceInformation().hashCode());
    }
}
