package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeteringDataFilterTest {

    private static EsPermissionRequest setupPermissionRequest(LocalDate expectedStartDate, @Nullable LocalDate expectedEndDate) {
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.start()).thenReturn(expectedStartDate.atStartOfDay(ZONE_ID_SPAIN));
        when(permissionRequest.end()).thenReturn(expectedEndDate == null ? null : expectedEndDate.atStartOfDay(ZONE_ID_SPAIN));
        return permissionRequest;
    }

    @Test
    void filter_reducesDataFrom7Days_to3Days() {
        LocalDate today = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate startDate = today.minusDays(3);
        LocalDate endDate = startDate.plusDays(6);
        var meteringData = createMeteringData(startDate, endDate);

        LocalDate expectedStartDate = today.minusDays(1);
        LocalDate expectedEndDate = expectedStartDate.plusDays(2);
        EsPermissionRequest permissionRequest = setupPermissionRequest(expectedStartDate, expectedEndDate);

        var filter = new MeteringDataFilter(meteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2)).orElseThrow();

        assertEquals(72, result.size());

        assertEquals(expectedStartDate, result.getFirst().dateTime().toLocalDate());
        assertEquals(LocalTime.MIN.plusHours(1), result.getFirst().dateTime().toLocalTime());
        assertEquals(expectedEndDate, result.getLast().dateTime().toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, result.getLast().dateTime().toLocalTime());
    }

    @Test
    void filter_ifEndNull_OnlyReducesStartDate() {
        LocalDate today = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate startDate = today.minusDays(3);
        LocalDate endDate = startDate.plusDays(6);
        var meteringData = createMeteringData(startDate, endDate);

        LocalDate expectedStartDate = today.minusDays(1);
        EsPermissionRequest permissionRequest = setupPermissionRequest(expectedStartDate, null);

        var filter = new MeteringDataFilter(meteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2)).orElseThrow();

        assertEquals(120, result.size());

        assertEquals(expectedStartDate, result.getFirst().dateTime().toLocalDate());
        assertEquals(LocalTime.MIN.plusHours(1), result.getFirst().dateTime().toLocalTime());
        assertEquals(endDate, result.getLast().dateTime().toLocalDate());
        assertEquals(LocalTime.MIDNIGHT, result.getLast().dateTime().toLocalTime());
    }

    @Test
    void filter_ifDataBeforePermission_returnsEmptyList() {
        LocalDate today = LocalDate.now(ZONE_ID_SPAIN);
        var meteringData = createMeteringData(today, today);

        LocalDate expectedStartDate = today.plusDays(2);
        LocalDate expectedEndDate = expectedStartDate.plusDays(1);
        EsPermissionRequest permissionRequest = setupPermissionRequest(expectedStartDate, expectedEndDate);

        var filter = new MeteringDataFilter(meteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2));

        assertTrue(result.isEmpty());
    }

    @Test
    void filter_ifDataAfterPermission_returnsEmptyList() {
        LocalDate today = LocalDate.now(ZONE_ID_SPAIN);
        var meteringData = createMeteringData(today, today);

        LocalDate expectedStartDate = today.minusDays(2);
        LocalDate expectedEndDate = expectedStartDate.plusDays(1);
        EsPermissionRequest permissionRequest = setupPermissionRequest(expectedStartDate, expectedEndDate);

        var filter = new MeteringDataFilter(meteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2));

        assertTrue(result.isEmpty());
    }

    private List<MeteringData> createMeteringData(LocalDate startDate, LocalDate endDate) {
        List<MeteringData> meteringData = new ArrayList<>();
        // iterate over dates
        startDate.datesUntil(endDate.plusDays(1)).forEach(date -> {
            // iterate over times
            for (LocalTime time = LocalTime.MIN.plusHours(1); !Objects.equals(time, LocalTime.MIDNIGHT); time = time.plusHours(1)) {
                meteringData.add(createMeteringData(date, time));
            }
            meteringData.add(createMeteringData(date, LocalTime.MIN));
        });

        return meteringData;
    }

    private MeteringData createMeteringData(LocalDate date, LocalTime time) {
        return new MeteringData("1", ZonedDateTime.of(date, time, ZONE_ID_SPAIN), 0.0, "REAL", 0.0);
    }
}