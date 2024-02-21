package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
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
class IdentifiableMeteringDataServiceTest {

    @Test
    void ifLastPulledEmpty_callsSetLastPulledMeterReading() {
        // Arrange
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.lastPulledMeterReading()).thenReturn(Optional.empty());

        MeteringData meteringData = new MeteringData("", ZonedDateTime.of(LocalDate.now(), LocalTime.MIN, ZONE_ID_SPAIN), 0.0, "REAL", 0.0);
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());
        // Act
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(permissionRequest, List.of(meteringData))))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest).setLastPulledMeterReading(meteringData.dateTime());
    }

    @Test
    void ifLastPulledPresent_callsSetLastPulledMeterReading_ifMeterReadingNewer() {
        // Arrange
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
        when(permissionRequest.lastPulledMeterReading()).thenReturn(Optional.of(now.minusDays(1)));

        MeteringData meteringData = new MeteringData("", ZonedDateTime.of(now.toLocalDate(), LocalTime.MIN, ZONE_ID_SPAIN), 0.0, "REAL", 0.0);
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());
        // Act
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(permissionRequest, List.of(meteringData))))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest).setLastPulledMeterReading(meteringData.dateTime());
    }

    @Test
    void ifLastPulledPresent_doesNotCallSetLastPulledMeterReading_ifMeterReadingOlder() {
        // Arrange
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
        when(permissionRequest.lastPulledMeterReading()).thenReturn(Optional.of(now));

        MeteringData meteringData = new MeteringData("", ZonedDateTime.of(now.minusDays(1).toLocalDate(), LocalTime.MIN, ZONE_ID_SPAIN), 0.0, "REAL", 0.0);
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());
        // Act
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(permissionRequest, List.of(meteringData))))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest, never()).setLastPulledMeterReading(any());
    }

    @Test
    void ifMeteringDataDateBeforePermissionEndDate_doesNotCallFulfill() throws StateTransitionException {
        // Arrange
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        ZonedDateTime now = LocalDate.now().atStartOfDay(ZONE_ID_SPAIN);
        when(permissionRequest.end()).thenReturn(now.plusDays(1));

        MeteringData meteringData = new MeteringData("", ZonedDateTime.of(now.minusDays(1).toLocalDate(), LocalTime.MIN, ZONE_ID_SPAIN), 0.0, "REAL", 0.0);
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());
        // Act
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(permissionRequest, List.of(meteringData))))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest, never()).fulfill();
    }

    @Test
    void ifMeteringDataDateAfterPermissionEndDate_callsFulfillOnlyOnce() throws StateTransitionException {
        // Arrange
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
        when(permissionRequest.end()).thenReturn(now.minusDays(1));

        MeteringData meteringData = new MeteringData("", ZonedDateTime.of(now.toLocalDate(), LocalTime.MIN, ZONE_ID_SPAIN), 0.0, "REAL", 0.0);
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());
        // Act
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(permissionRequest, List.of(meteringData, meteringData))))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest, times(1)).fulfill();
    }

    @Test
    void ifMeteringDataDateEqualsPermissionEndDate_callsFulfill() throws StateTransitionException {
        // Arrange
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        ZonedDateTime now = LocalDate.now().atStartOfDay(ZONE_ID_SPAIN);
        when(permissionRequest.end()).thenReturn(now);

        MeteringData meteringData = new MeteringData("", now, 0.0, "REAL", 0.0);
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());
        // Act
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(permissionRequest, List.of(meteringData))))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest).fulfill();
    }
}