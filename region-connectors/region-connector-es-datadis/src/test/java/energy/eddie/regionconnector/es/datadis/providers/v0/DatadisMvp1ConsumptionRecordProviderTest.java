package energy.eddie.regionconnector.es.datadis.providers.v0;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.ObtainMethod;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DatadisMvp1ConsumptionRecordProviderTest {
    @Test
    void givenValueOnFlux_publishesConsumptionRecordOnFlow() {
        // Given
        var reading = createReading();
        TestPublisher<IdentifiableMeteringData> publisher = TestPublisher.create();

        //noinspection resource StepVerifier closes provider
        var provider = new DatadisMvp1ConsumptionRecordProvider(publisher.flux());

        StepVerifier.create(provider.getConsumptionRecordStream())
                    // When
                    .then(() -> publisher.next(reading))
                    // Then
                    .assertNext(consumptionRecord -> assertAll(
                            () -> assertEquals(reading.permissionRequest().connectionId(),
                                               consumptionRecord.getConnectionId()),
                            () -> assertEquals(reading.permissionRequest().permissionId(),
                                               consumptionRecord.getPermissionId()),
                            () -> assertEquals(reading.permissionRequest().dataNeedId(),
                                               consumptionRecord.getDataNeedId()),
                            () -> assertEquals(1, consumptionRecord.getConsumptionPoints().size())
                    ))
                    .thenCancel()
                    .verify(Duration.ofSeconds(2));
    }

    private IdentifiableMeteringData createReading() {
        var today = LocalDate.now(ZONE_ID_SPAIN);
        var end = today.plusDays(1);
        StateBuilderFactory stateBuilderFactory = new StateBuilderFactory(null);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation(
                "connectionId",
                "dataNeedId",
                "nif",
                "meteringPointId"
        );
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest("permissionId",
                                                                             permissionRequestForCreation,
                                                                             today,
                                                                             end,
                                                                             Granularity.PT1H,
                                                                             stateBuilderFactory);
        permissionRequest.changeState(stateBuilderFactory.create(permissionRequest, PermissionProcessStatus.ACCEPTED)
                                                         .build());

        var meteringData = new MeteringData("CUPS",
                                            end.plusDays(1),
                                            "00:00",
                                            123.123,
                                            ObtainMethod.REAL,
                                            0);
        return new IdentifiableMeteringData(permissionRequest,
                                            IntermediateMeteringData.fromMeteringData(List.of(meteringData)));
    }
}
