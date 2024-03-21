package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class LastPulledMeterReadingServiceTest {
    private static final LocalDate today = LocalDate.now(ZONE_ID_SPAIN);

    @Test
    void ifLastPulledEmpty_callsSetLastPulledMeterReading() {
        // Given
        LocalDate end = today.plusDays(1);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(today, end);

        LastPulledMeterReadingService lastPulledMeterReadingService = new LastPulledMeterReadingService();

        // When
        lastPulledMeterReadingService.updateLastPulledMeterReading(permissionRequest, end);

        // Then
        assertEquals(end, permissionRequest.lastPulledMeterReading().get());
    }

    private static EsPermissionRequest acceptedPermissionRequest(LocalDate start, LocalDate end) {
        StateBuilderFactory stateBuilderFactory = new StateBuilderFactory(null);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation(
                "connectionId",
                "dataNeedId",
                "nif",
                "meteringPointId");
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest("permissionId",
                                                                             permissionRequestForCreation,
                                                                             start,
                                                                             end,
                                                                             Granularity.PT1H,
                                                                             stateBuilderFactory);
        permissionRequest.changeState(stateBuilderFactory.create(permissionRequest, PermissionProcessStatus.ACCEPTED)
                                                         .build());
        return permissionRequest;
    }

    @Test
    void ifLastPulledPresent_callsSetLastPulledMeterReading_ifMeterReadingNewer() {
        // Given
        LocalDate start = today.minusDays(2);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, today);
        permissionRequest.updateLastPulledMeterReading(today.minusDays(1));

        LocalDate end = today;

        LastPulledMeterReadingService lastPulledMeterReadingService = new LastPulledMeterReadingService();

        // When
        lastPulledMeterReadingService.updateLastPulledMeterReading(permissionRequest, end);
        // Then
        assertEquals(end, permissionRequest.lastPulledMeterReading().get());
    }

    @Test
    void ifLastPulledPresent_doesNotCallSetLastPulledMeterReading_ifMeterReadingOlder() {
        // Given
        LocalDate start = today.minusDays(2);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, today);
        permissionRequest.updateLastPulledMeterReading(today);

        LastPulledMeterReadingService lastPulledMeterReadingService = new LastPulledMeterReadingService();

        // When
        lastPulledMeterReadingService.updateLastPulledMeterReading(permissionRequest, today);

        // Then
        assertEquals(today, permissionRequest.lastPulledMeterReading().get());
    }
}
