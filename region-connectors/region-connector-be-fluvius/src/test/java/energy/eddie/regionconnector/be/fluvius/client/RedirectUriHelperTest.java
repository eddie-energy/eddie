package energy.eddie.regionconnector.be.fluvius.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedirectUriHelperTest {

    @Test
    void testSuccessUri_returnsCorrectUri() {
        // Given
        var helper = new RedirectUriHelper("https://localhost:8080");

        // When
        var res = helper.successUri("pid");

        // Then
        assertEquals("https://localhost:8080/region-connectors/be-fluvius/permission-request/pid/accepted", res);
    }

    @Test
    void testRejectUri_returnsCorrectUri() {
        // Given
        var helper = new RedirectUriHelper("https://localhost:8080");

        // When
        var res = helper.rejectUri("pid");

        // Then
        assertEquals("https://localhost:8080/region-connectors/be-fluvius/permission-request/pid/rejected", res);
    }
}