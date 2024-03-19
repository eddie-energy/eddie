package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class LastPulledMeterReadingServiceTest {
    private static final ZonedDateTime today = LocalDate.now(ZONE_ID_SPAIN).atStartOfDay(ZONE_ID_SPAIN);

    private static EsPermissionRequest acceptedPermissionRequest(ZonedDateTime start, ZonedDateTime end) {
        StateBuilderFactory stateBuilderFactory = new StateBuilderFactory(null);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation(
                "connectionId",
                "dataNeedId",
                "nif",
                "meteringPointId",
                start,
                end,
                Granularity.PT1H);
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest("permissionId", permissionRequestForCreation, stateBuilderFactory);
        permissionRequest.changeState(stateBuilderFactory.create(permissionRequest, PermissionProcessStatus.ACCEPTED).build());
        return permissionRequest;
    }


    @Test
    void ifLastPulledEmpty_callsSetLastPulledMeterReading() {
        // Given
        ZonedDateTime end = today.plusDays(1);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(today, end);

        LastPulledMeterReadingService lastPulledMeterReadingService = new LastPulledMeterReadingService();

        // When
        lastPulledMeterReadingService.updateLastPulledMeterReading(permissionRequest, end);

        // Then
        assertEquals(end, permissionRequest.lastPulledMeterReading().get());
    }

    @Test
    void ifLastPulledPresent_callsSetLastPulledMeterReading_ifMeterReadingNewer() {
        // Given
        ZonedDateTime start = ZonedDateTime.of(today.toLocalDate().minusDays(2), LocalTime.MIN, ZONE_ID_SPAIN);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, today);
        permissionRequest.setLastPulledMeterReading(today.minusDays(1));

        ZonedDateTime end = ZonedDateTime.of(today.toLocalDate(), LocalTime.MIN, ZONE_ID_SPAIN);

        LastPulledMeterReadingService lastPulledMeterReadingService = new LastPulledMeterReadingService();

        // When
        lastPulledMeterReadingService.updateLastPulledMeterReading(permissionRequest, end);
        // Then
        assertEquals(end, permissionRequest.lastPulledMeterReading().get());
    }

    @Test
    void ifLastPulledPresent_doesNotCallSetLastPulledMeterReading_ifMeterReadingOlder() {
        // Given
        ZonedDateTime start = ZonedDateTime.of(today.toLocalDate().minusDays(2), LocalTime.MIN, ZONE_ID_SPAIN);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, today);
        permissionRequest.setLastPulledMeterReading(today);
        ZonedDateTime end = ZonedDateTime.of(today.toLocalDate(), LocalTime.MIN, ZONE_ID_SPAIN);

        LastPulledMeterReadingService lastPulledMeterReadingService = new LastPulledMeterReadingService();

        // When
        lastPulledMeterReadingService.updateLastPulledMeterReading(permissionRequest, end);

        // Then
        assertEquals(today, permissionRequest.lastPulledMeterReading().get());
    }
}
