// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.admin.console.data;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusMessageDTOTest {
    @Test
    void testGetParsedStartDate() {
        // Given
        String startDate = "2024-05-22T08:20:03+02:00";
        StatusMessageDTO statusMessageDTO = new StatusMessageDTO("testCountry", "testRegionConnectorId", "testDataNeedId", "testDso", "testPermissionId", startDate, "CREATED", "A05");

        // When
        ZonedDateTime parsedStartDate = statusMessageDTO.getParsedStartDate();

        // Then
        ZonedDateTime expectedDate = ZonedDateTime.parse(startDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(expectedDate, parsedStartDate);
    }
}
