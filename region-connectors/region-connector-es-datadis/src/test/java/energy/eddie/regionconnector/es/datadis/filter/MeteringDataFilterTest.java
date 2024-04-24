package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;

class MeteringDataFilterTest {

    private static IntermediateMeteringData hourlyIntermediateMeteringData;
    private static IntermediateMeteringData quarterHourlyIntermediateMeteringData;
    private static IntermediateMeteringData hourlyIntermediateMeteringDataWithDaylightSavings;

    @BeforeAll
    static void setUp() throws IOException {
        List<MeteringData> meteringData = MeteringDataProvider.loadMeteringData();
        List<MeteringData> meteringDataWithDaylightSavings = MeteringDataProvider.loadMeteringDataWithDaylightSavings();
        List<MeteringData> quarterHourlyMeteringData = fromHourlyMeteringData(meteringData);
        quarterHourlyIntermediateMeteringData = IntermediateMeteringData.fromMeteringData(quarterHourlyMeteringData);
        hourlyIntermediateMeteringData = IntermediateMeteringData.fromMeteringData(meteringData);
        hourlyIntermediateMeteringDataWithDaylightSavings = IntermediateMeteringData.fromMeteringData(
                meteringDataWithDaylightSavings);
    }

    private static List<MeteringData> fromHourlyMeteringData(List<MeteringData> meteringData) {
        List<String> minutes = List.of("15", "30", "45");
        List<MeteringData> quarterHourlyMeteringData = new ArrayList<>();
        int currentHour = 0;
        for (MeteringData data : meteringData) {
            for (String minute : minutes) {
                String time = String.format("%02d:%s", currentHour, minute);
                quarterHourlyMeteringData.add(new MeteringData(data.cups(),
                                                               data.date(),
                                                               time,
                                                               data.consumptionKWh(),
                                                               data.obtainMethod(),
                                                               data.surplusEnergyKWh()));
            }
            currentHour++;
            quarterHourlyMeteringData.add(data);
            if (currentHour == 24) {
                currentHour = 0;
            }
        }
        return quarterHourlyMeteringData;
    }

    @ParameterizedTest
    @EnumSource(MeasurementType.class)
    void filter_reducesDataFrom1Month_to3Days(MeasurementType measurementType) {
        var intermediateMeteringData = setupIntermediateMeteringData(measurementType);
        LocalDate requestedStartDate = intermediateMeteringData.start().plusWeeks(2);
        LocalDate requestedEndDate = requestedStartDate.plusDays(2); // plus 2 since the end date is inclusive
        ZonedDateTime expectedEnd = requestedEndDate.plusDays(1).atStartOfDay(ZONE_ID_SPAIN);
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate, requestedEndDate,
                                                                       measurementType);
        int expectedSize = switch (measurementType) {
            case HOURLY -> 72;
            case QUARTER_HOURLY -> 72 * 4;
        };
        ZonedDateTime expectedStart = expectedStart(measurementType, requestedStartDate);


        var filter = new MeteringDataFilter(intermediateMeteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2)).orElseThrow();

        assertAll(
                () -> assertEquals(expectedSize, result.meteringData().size()),
                () -> assertEquals(expectedStart.toLocalDate(), result.start()),
                () -> assertEquals(expectedStart.toLocalDate(),
                                   result.meteringData().getFirst().date()),
                () -> assertEquals(expectedStart.toLocalTime().toString(), result.meteringData().getFirst().time()),
                () -> assertEquals(expectedEnd.toLocalDate(), result.end()),
                () -> assertEquals(expectedEnd.toLocalDate().minusDays(1),
                                   result.meteringData().getLast().date()),
                () -> assertEquals("24:00", result.meteringData().getLast().time())
        );
    }

    private IntermediateMeteringData setupIntermediateMeteringData(MeasurementType measurementType) {
        return switch (measurementType) {
            case HOURLY -> hourlyIntermediateMeteringData;
            case QUARTER_HOURLY -> quarterHourlyIntermediateMeteringData;
        };
    }

    private static EsPermissionRequest setupPermissionRequest(
            LocalDate requestedStartDate,
            LocalDate requestedEndDate,
            MeasurementType measurementType
    ) {
        var granularity = switch (measurementType) {
            case HOURLY -> Granularity.PT1H;
            case QUARTER_HOURLY -> Granularity.PT15M;
        };
        return new DatadisPermissionRequest(
                "1",
                "1",
                "1",
                granularity,
                "1",
                "1",
                requestedStartDate,
                requestedEndDate,
                null,
                null,
                null,
                PermissionProcessStatus.ACCEPTED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
    }

    private static ZonedDateTime expectedStart(MeasurementType measurementType, LocalDate requestedStartDate) {
        return switch (measurementType) {
            case HOURLY -> requestedStartDate.atStartOfDay(ZONE_ID_SPAIN).plusHours(1);
            case QUARTER_HOURLY -> requestedStartDate.atStartOfDay(ZONE_ID_SPAIN).plusMinutes(15);
        };
    }

    @Test
    void filter_containingDaylightSavings() {
        var intermediateMeteringData = hourlyIntermediateMeteringDataWithDaylightSavings;
        LocalDate requestedStartDate = LocalDate.of(2023, 10, 25); // DST ends 2023/10/29
        LocalDate requestedEndDate = LocalDate.of(2024, 4, 2); // DST starts 2024/03/31
        ZonedDateTime expectedEnd = requestedEndDate.plusDays(1).atStartOfDay(ZONE_ID_SPAIN);
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate, requestedEndDate,
                                                                       MeasurementType.HOURLY);
        ZonedDateTime expectedStart = expectedStart(MeasurementType.HOURLY, requestedStartDate);

        //benchmark filter
        var filter = new MeteringDataFilter(intermediateMeteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2)).orElseThrow();
        assertAll(
                () -> assertEquals(expectedStart.toLocalDate(), result.start()),
                () -> assertEquals(expectedStart.toLocalDate(),
                                   result.meteringData().getFirst().date()),
                () -> assertEquals(expectedStart.toLocalTime().toString(), result.meteringData().getFirst().time()),
                () -> assertEquals(expectedEnd.toLocalDate(), result.end()),
                () -> assertEquals(expectedEnd.toLocalDate().minusDays(1),
                                   result.meteringData().getLast().date()),
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
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate,
                                                                       requestedEndDate,
                                                                       measurementType);

        var filter = new MeteringDataFilter(intermediateMeteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2));

        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(MeasurementType.class)
    void filter_ifDataAfterPermission_returnsEmptyList(MeasurementType measurementType) {
        var intermediateMeteringData = setupIntermediateMeteringData(measurementType);
        LocalDate requestedStartDate = intermediateMeteringData.start().minusDays(2);
        LocalDate requestedEndDate = requestedStartDate.plusDays(1);
        EsPermissionRequest permissionRequest = setupPermissionRequest(requestedStartDate,
                                                                       requestedEndDate,
                                                                       measurementType);

        var filter = new MeteringDataFilter(intermediateMeteringData, permissionRequest);
        var result = filter.filter().blockOptional(Duration.ofSeconds(2));

        assertTrue(result.isEmpty());
    }
}
