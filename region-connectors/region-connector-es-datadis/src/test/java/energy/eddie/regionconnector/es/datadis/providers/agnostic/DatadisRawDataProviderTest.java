package energy.eddie.regionconnector.es.datadis.providers.agnostic;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;

class DatadisRawDataProviderTest {
    @Test
    void completeOnInputFlux_emitsCompleteOnRawDataFlow() {
        TestPublisher<IdentifiableMeteringData> publisher = TestPublisher.create();
        //noinspection resource StepVerifier closes provider
        var provider = new DatadisRawDataProvider(publisher.flux());

        StepVerifier.create(provider.getRawDataStream())
                .then(publisher::complete)
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void givenValueOnFlux_publishesOnFlow() {
        // Given
        TestPublisher<IdentifiableMeteringData> publisher = TestPublisher.create();
        var reading = createReading();

        //noinspection resource StepVerifier closes provider
        var provider = new DatadisRawDataProvider(publisher.flux());

        StepVerifier.create(provider.getRawDataStream())
                // When
                .then(() -> publisher.next(reading))
                // Then
                .expectNextCount(1)
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    private IdentifiableMeteringData createReading() {
        StateBuilderFactory stateBuilderFactory = new StateBuilderFactory(null);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation(
                "connectionId",
                "dataNeedId",
                "nif",
                "meteringPointId",
                ZonedDateTime.now(ZONE_ID_SPAIN),
                ZonedDateTime.now(ZONE_ID_SPAIN).plusDays(1),
                Granularity.PT1H);
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest("permissionId", permissionRequestForCreation, stateBuilderFactory);
        permissionRequest.changeState(stateBuilderFactory.create(permissionRequest, PermissionProcessStatus.ACCEPTED).build());

        return new IdentifiableMeteringData(permissionRequest, new IntermediateMeteringData(List.of(), permissionRequest.start(), permissionRequest.end()));
    }
}
