package energy.eddie.admin.console.data;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatusMessageTest {
    @Test
    void testGetParsedStartDate() {
        // Given
        String startDate = "2024-05-22T08:20:03+02:00";
        StatusMessage statusMessage = new StatusMessage("testPermissionId", "testCountry", "testDso", startDate, "A05");

        // When
        LocalDate parsedStartDate = statusMessage.getParsedStartDate();

        // Then
        LocalDate expectedDate = LocalDate.of(2024, 5, 22);
        assertEquals(expectedDate, parsedStartDate);
    }

    @Test
    void testNoArgConstructor() {
        // When
        StatusMessage statusMessage = new StatusMessage();

        // Then
        assertNull(statusMessage.getId());
        assertNull(statusMessage.getPermissionId());
        assertNull(statusMessage.getCountry());
        assertNull(statusMessage.getDso());
        assertNull(statusMessage.getStartDate());
        assertNull(statusMessage.getStatus());
    }

}
