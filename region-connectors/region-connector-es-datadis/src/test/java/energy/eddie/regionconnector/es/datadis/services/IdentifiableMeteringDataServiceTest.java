package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDate;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.mockito.Mockito.*;

class IdentifiableMeteringDataServiceTest {

    private static final LocalDate today = LocalDate.now(ZONE_ID_SPAIN);

    private static EsPermissionRequest acceptedPermissionRequest(LocalDate start, LocalDate end) {
        StateBuilderFactory stateBuilderFactory = new StateBuilderFactory(null);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation(
                "connectionId",
                "dataNeedId",
                "nif",
                "meteringPointId",
                start.atStartOfDay(ZONE_ID_SPAIN),
                end.atStartOfDay(ZONE_ID_SPAIN),
                Granularity.PT1H);
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest("permissionId", permissionRequestForCreation, stateBuilderFactory);
        permissionRequest.changeState(stateBuilderFactory.create(permissionRequest, PermissionProcessStatus.ACCEPTED).build());
        return permissionRequest;
    }

    @Test
    void ifLastPulledEmpty_callsSetLastPulledMeterReading() {
        // Given
        LocalDate end = today.plusDays(1);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(today, end);
        EsPermissionRequest spy = spy(permissionRequest);

        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        //noinspection unused
        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());

        // When
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(spy, new IntermediateMeteringData(List.of(), today, end))))
                .expectNextCount(1)
                .verifyComplete();
        // Then
        verify(spy).updateLastPulledMeterReading(end);
    }

    @Test
    void ifLastPulledPresent_callsSetLastPulledMeterReading_ifMeterReadingNewer() {
        // Given
        LocalDate start = today.minusDays(2);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, today);
        permissionRequest.updateLastPulledMeterReading(today.minusDays(1));
        EsPermissionRequest spy = spy(permissionRequest);

        LocalDate end = today;
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        //noinspection unused
        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());

        // When
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(spy, new IntermediateMeteringData(List.of(), start, end))))
                .expectNextCount(1)
                .verifyComplete();
        // Then
        verify(spy).updateLastPulledMeterReading(end);
    }

    @Test
    void ifLastPulledPresent_doesNotCallSetLastPulledMeterReading_ifMeterReadingOlder() {
        // Given
        LocalDate start = today.minusDays(2);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, today);
        permissionRequest.updateLastPulledMeterReading(today);
        EsPermissionRequest spy = spy(permissionRequest);

        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        //noinspection unused
        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());

        // When
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(spy, new IntermediateMeteringData(List.of(), start, today))))
                .expectNextCount(1)
                .verifyComplete();
        // Then
        verify(spy, never()).updateLastPulledMeterReading(any());
    }

    @Test
    void ifMeteringDataDateBeforePermissionEndDate_doesNotCallFulfill() throws StateTransitionException {
        // Given
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(today.minusDays(2), today.minusDays(1));
        EsPermissionRequest spy = spy(permissionRequest);

        LocalDate start = today.minusDays(2);
        LocalDate end = today.minusDays(1);
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        //noinspection unused
        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());

        // When
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(spy, new IntermediateMeteringData(List.of(), start, end))))
                .expectNextCount(1)
                .verifyComplete();
        // Then
        verify(spy, never()).fulfill();
    }

    @Test
    void ifMeteringDataDateAfterPermissionEndDate_callsFulfillOnlyOnce() throws StateTransitionException {
        // Given
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(today.minusDays(2), today.minusDays(1));
        EsPermissionRequest spy = spy(permissionRequest);
        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        //noinspection unused
        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());

        // When
        StepVerifier.create(testPublisher)
                .then(() -> {
                    var identifiableMeteringData = new IdentifiableMeteringData(spy, new IntermediateMeteringData(List.of(), today.minusDays(2), today));
                    testPublisher.emit(identifiableMeteringData, identifiableMeteringData);
                })
                .expectNextCount(2)
                .verifyComplete();
        // Then
        verify(spy, times(1)).fulfill();
    }

    @Test
    void ifMeteringDataDateEqualsPermissionEndDate_callsFulfill() throws StateTransitionException {
        // Given
        LocalDate start = today.minusDays(2);
        EsPermissionRequest permissionRequest = acceptedPermissionRequest(start, today.minusDays(1));
        EsPermissionRequest spy = spy(permissionRequest);

        TestPublisher<IdentifiableMeteringData> testPublisher = TestPublisher.create();

        //noinspection unused
        IdentifiableMeteringDataService identifiableMeteringDataService = new IdentifiableMeteringDataService(testPublisher.flux());

        // When
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeteringData(spy, new IntermediateMeteringData(List.of(), start, today))))
                .expectNextCount(1)
                .verifyComplete();
        // Then
        verify(spy).fulfill();
    }
}
