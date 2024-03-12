package energy.eddie.regionconnector.es.datadis.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SuppressWarnings("DataFlowIssue")
class MeteringDataFilterTest {

    private static List<MeteringData> meteringData;

    private static ZonedDateTime meteringDataStartDate;
    private static ZonedDateTime meteringDataEndDate;

    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream is = MeteringDataFilterTest.class.getClassLoader().getResourceAsStream("consumptionKWh.json")) {
            meteringData = objectMapper.readValue(is, new TypeReference<>() {
            });
            meteringDataStartDate = meteringData.getFirst().dateTime();
            meteringDataEndDate = meteringData.getLast().dateTime();
        }
    }

    private static EsPermissionRequest setupPermissionRequest(LocalDate requestedStartDate, @Nullable LocalDate requestedEndDate) {
        return new DatadisPermissionRequest("1",
                new PermissionRequestForCreation("1", "1", "1", "1", requestedStartDate.atStartOfDay(ZONE_ID_SPAIN),
                        requestedEndDate == null ? null : requestedEndDate.atStartOfDay(ZONE_ID_SPAIN), Granularity.PT1H),
                new StateBuilderFactory(mock(AuthorizationApi.class)));
    }

    @Test
    void filter_reducesDataFrom1Month_to3Days() {
        LocalDate requestedStartDate = meteringDataStartDate.plusWeeks(2).toLocalDate();
        LocalDate requestedEndDate = requestedStartDate.plusDays(2);
        ZonedDateTime expectedStart = requestedStartDate.atStartOfDay(ZONE_ID_SPAIN).plusHours(1);
        ZonedDateTime expectedEnd = requestedEndDate.atStartOfDay(ZONE_ID_SPAIN).plusDays(1);
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate, requestedEndDate);

        var filter = new MeteringDataFilter(meteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2)).orElseThrow();

        assertEquals(72, result.size());


        assertEquals(expectedStart, result.getFirst().dateTime());
        assertEquals(expectedEnd, result.getLast().dateTime());
    }

    @Test
    void filter_ifEndNull_OnlyReducesStartDate() {
        LocalDate requestedStartDate = meteringDataEndDate.minusDays(5).toLocalDate();
        ZonedDateTime expectedStart = requestedStartDate.atStartOfDay(ZONE_ID_SPAIN).plusHours(1);
        ZonedDateTime expectedEnd = meteringDataEndDate;
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate, null);

        var filter = new MeteringDataFilter(meteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2)).orElseThrow();

        assertEquals(120, result.size());

        assertEquals(expectedStart, result.getFirst().dateTime());
        assertEquals(expectedEnd, result.getLast().dateTime());
    }

    @Test
    void filter_ifDataBeforePermission_returnsEmptyList() {
        LocalDate today = LocalDate.now(ZONE_ID_SPAIN);

        LocalDate requestedStartDate = today.plusDays(2);
        LocalDate requestedEndDate = requestedStartDate.plusDays(1);
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate, requestedEndDate);

        var filter = new MeteringDataFilter(meteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2));

        assertTrue(result.isEmpty());
    }

    @Test
    void filter_ifDataAfterPermission_returnsEmptyList() {
        LocalDate requestedStartDate = meteringDataStartDate.minusDays(2).toLocalDate();
        LocalDate requestedEndDate = requestedStartDate.plusDays(1);
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate, requestedEndDate);

        var filter = new MeteringDataFilter(meteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2));

        assertTrue(result.isEmpty());
    }
}
