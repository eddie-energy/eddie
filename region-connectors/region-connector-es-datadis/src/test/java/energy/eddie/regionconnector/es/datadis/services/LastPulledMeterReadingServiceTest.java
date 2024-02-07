package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.mockito.Mockito.*;

@SuppressWarnings("unused")
class LastPulledMeterReadingServiceTest {

    @Test
    void ifLastPulledEmpty_callsSetLastPulledMeterReading() {
        // Arrange
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.lastPulledMeterReading()).thenReturn(Optional.empty());

        MeteringData meteringData = new MeteringData("", LocalDate.now(ZONE_ID_SPAIN), LocalTime.MIN, 0.0, "REAL", 0.0);
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        LastPulledMeterReadingService lastPulledMeterReadingService = new LastPulledMeterReadingService(testPublisher.flux());
        // Act
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(permissionRequest, List.of(meteringData))))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest).setLastPulledMeterReading(meteringData.date().atStartOfDay(ZONE_ID_SPAIN));
    }

    @Test
    void ifLastPulledPresent_callsSetLastPulledMeterReading_ifMeterReadingNewer() {
        // Arrange
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
        when(permissionRequest.lastPulledMeterReading()).thenReturn(Optional.of(now.minusDays(1)));

        MeteringData meteringData = new MeteringData("", now.toLocalDate(), LocalTime.MIN, 0.0, "REAL", 0.0);
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        LastPulledMeterReadingService lastPulledMeterReadingService = new LastPulledMeterReadingService(testPublisher.flux());
        // Act
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(permissionRequest, List.of(meteringData))))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest).setLastPulledMeterReading(meteringData.date().atStartOfDay(ZONE_ID_SPAIN));
    }

    @Test
    void ifLastPulledPresent_doesNotCallSetLastPulledMeterReading_ifMeterReadingOlder() {
        // Arrange
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
        when(permissionRequest.lastPulledMeterReading()).thenReturn(Optional.of(now));

        MeteringData meteringData = new MeteringData("", now.minusDays(1).toLocalDate(), LocalTime.MIN, 0.0, "REAL", 0.0);
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        LastPulledMeterReadingService lastPulledMeterReadingService = new LastPulledMeterReadingService(testPublisher.flux());
        // Act
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(permissionRequest, List.of(meteringData))))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest, never()).setLastPulledMeterReading(any());
    }
}