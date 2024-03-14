package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SuppressWarnings("DataFlowIssue")
class MeteringDataFilterTest {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static IntermediateMeteringData hourlyIntermediateMeteringData;
    private static IntermediateMeteringData quarterHourlyIntermediateMeteringData;

    @BeforeAll
    static void setUp() throws IOException {
        List<MeteringData> meteringData = MeteringDataProvider.loadMeteringData();
        List<MeteringData> quarterHourlyMeteringData = fromHourlyMeteringData(meteringData);
        quarterHourlyIntermediateMeteringData = IntermediateMeteringData.fromMeteringData(quarterHourlyMeteringData);
        hourlyIntermediateMeteringData = IntermediateMeteringData.fromMeteringData(meteringData);
    }

    private static List<MeteringData> fromHourlyMeteringData(List<MeteringData> meteringData) {
        List<String> minutes = List.of("15", "30", "45");
        List<MeteringData> quarterHourlyMeteringData = new ArrayList<>();
        int currentHour = 0;
        for (MeteringData data : meteringData) {
            for (String minute : minutes) {
                String time = String.format("%02d:%s", currentHour, minute);
                quarterHourlyMeteringData.add(new MeteringData(data.cups(), data.date(), time, data.consumptionKWh(), data.obtainMethod(), data.surplusEnergyKWh()));
            }
            currentHour++;
            quarterHourlyMeteringData.add(data);
            if (currentHour == 24) {
                currentHour = 0;
            }
        }
        return quarterHourlyMeteringData;
    }

    private static EsPermissionRequest setupPermissionRequest(LocalDate requestedStartDate, @Nullable LocalDate requestedEndDate, MeasurementType measurementType) {
        return new DatadisPermissionRequest(
                "1",
                new PermissionRequestForCreation("1", "1", "1", "1",
                        requestedStartDate.atStartOfDay(ZONE_ID_SPAIN),
                        requestedEndDate == null ? null : requestedEndDate.atStartOfDay(ZONE_ID_SPAIN),
                        switch (measurementType) {
                            case HOURLY -> Granularity.PT1H;
                            case QUARTER_HOURLY -> Granularity.PT15M;
                        }
                ),
                new StateBuilderFactory(mock(AuthorizationApi.class)
                )
        );
    }

    private static ZonedDateTime expectedStart(MeasurementType measurementType, LocalDate requestedStartDate) {
        return switch (measurementType) {
            case HOURLY -> requestedStartDate.atStartOfDay(ZONE_ID_SPAIN).plusHours(1);
            case QUARTER_HOURLY -> requestedStartDate.atStartOfDay(ZONE_ID_SPAIN).plusMinutes(15);
        };
    }

    @ParameterizedTest
    @EnumSource(MeasurementType.class)
    void filter_reducesDataFrom1Month_to3Days(MeasurementType measurementType) {
        var intermediateMeteringData = setupIntermediateMeteringData(measurementType);
        LocalDate requestedStartDate = intermediateMeteringData.start().plusWeeks(2).toLocalDate();
        LocalDate requestedEndDate = requestedStartDate.plusDays(2);
        ZonedDateTime expectedEnd = requestedEndDate.atStartOfDay(ZONE_ID_SPAIN).plusDays(1);
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate, requestedEndDate, measurementType);
        int expectedSize = switch (measurementType) {
            case HOURLY -> 72;
            case QUARTER_HOURLY -> 72 * 4;
        };
        ZonedDateTime expectedStart = expectedStart(measurementType, requestedStartDate);


        var filter = new MeteringDataFilter(intermediateMeteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2)).orElseThrow();

        assertAll(
                () -> assertEquals(expectedSize, result.meteringData().size()),
                () -> assertEquals(expectedStart, result.start()),
                () -> assertEquals(expectedStart.toLocalDate().format(DATE_FORMAT), result.meteringData().getFirst().date()),
                () -> assertEquals(expectedStart.toLocalTime().toString(), result.meteringData().getFirst().time()),
                () -> assertEquals(expectedEnd, result.end()),
                () -> assertEquals(expectedEnd.toLocalDate().minusDays(1).format(DATE_FORMAT), result.meteringData().getLast().date()),
                () -> assertEquals("24:00", result.meteringData().getLast().time())
        );
    }

    private IntermediateMeteringData setupIntermediateMeteringData(MeasurementType measurementType) {
        return switch (measurementType) {
            case HOURLY -> hourlyIntermediateMeteringData;
            case QUARTER_HOURLY -> quarterHourlyIntermediateMeteringData;
        };
    }

    @ParameterizedTest
    @EnumSource(MeasurementType.class)
    void filter_ifEndNull_OnlyReducesStartDate(MeasurementType measurementType) {
        var intermediateMeteringData = setupIntermediateMeteringData(measurementType);
        LocalDate requestedStartDate = intermediateMeteringData.end().minusDays(5).toLocalDate();
        ZonedDateTime expectedEnd = intermediateMeteringData.end();
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate, null, measurementType);

        ZonedDateTime expectedStart = expectedStart(measurementType, requestedStartDate);
        int expectedSize = switch (measurementType) {
            case HOURLY -> 120;
            case QUARTER_HOURLY -> 120 * 4;
        };

        var filter = new MeteringDataFilter(intermediateMeteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2)).orElseThrow();

        assertAll(
                () -> assertEquals(expectedSize, result.meteringData().size()),
                () -> assertEquals(expectedStart, result.start()),
                () -> assertEquals(expectedStart.toLocalDate().format(DATE_FORMAT), result.meteringData().getFirst().date()),
                () -> assertEquals(expectedStart.toLocalTime().toString(), result.meteringData().getFirst().time()),
                () -> assertEquals(expectedEnd, result.end()),
                () -> assertEquals(expectedEnd.toLocalDate().minusDays(1).format(DATE_FORMAT), result.meteringData().getLast().date()),
                () -> assertEquals("24:00", result.meteringData().getLast().time())
        );
    }

    @ParameterizedTest
    @EnumSource(MeasurementType.class)
    void filter_ifDataBeforePermission_returnsEmptyList(MeasurementType measurementType) {
        var intermediateMeteringData = setupIntermediateMeteringData(measurementType);
        LocalDate today = LocalDate.now(ZONE_ID_SPAIN);

        LocalDate requestedStartDate = today.plusDays(2);
        LocalDate requestedEndDate = requestedStartDate.plusDays(1);
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate, requestedEndDate, measurementType);

        var filter = new MeteringDataFilter(intermediateMeteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2));

        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(MeasurementType.class)
    void filter_ifDataAfterPermission_returnsEmptyList(MeasurementType measurementType) {
        var intermediateMeteringData = setupIntermediateMeteringData(measurementType);
        LocalDate requestedStartDate = intermediateMeteringData.start().minusDays(2).toLocalDate();
        LocalDate requestedEndDate = requestedStartDate.plusDays(1);
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate, requestedEndDate, measurementType);

        var filter = new MeteringDataFilter(intermediateMeteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2));

        assertTrue(result.isEmpty());
    }
}
