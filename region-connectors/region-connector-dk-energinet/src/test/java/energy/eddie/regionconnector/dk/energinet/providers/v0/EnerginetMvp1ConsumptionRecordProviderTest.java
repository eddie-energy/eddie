package energy.eddie.regionconnector.dk.energinet.providers.v0;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnerginetMvp1ConsumptionRecordProviderTest {
    @Test
    void givenValueOnFlux_publishesConsumptionRecordOnFlow() {
        // Given
        TestPublisher<IdentifiableApiResponse> publisher = TestPublisher.create();
        ZonedDateTime start = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.of(2023, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        var provider = new EnerginetMvp1ConsumptionRecordProvider(publisher.flux());
        var document = new MyEnergyDataMarketDocument()
                .periodTimeInterval(
                        new PeriodtimeInterval()
                                .start(start.toString())
                                .end(end.toLocalDate().toString())
                )
                .addTimeSeriesItem(
                        new TimeSeries()
                                .addPeriodItem(
                                        new Period()
                                                .resolution("PT15M")
                                                .addPointItem(
                                                        new Point()
                                                                .outQuantityQuality("A04")
                                                                .outQuantityQuantity(Double.toString(0.0))
                                                )
                                )
                );
        var responseDocument = new MyEnergyDataMarketDocumentResponse();
        responseDocument.setId("ID");
        responseDocument.setMyEnergyDataMarketDocument(document);
        var permissionRequest = new EnerginetPermissionRequest(
                "pId",
                "conId",
                "dId",
                "meteringPointId",
                "refreshToken",
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                Granularity.PT1H,
                "accessToken",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        var apiResponse = new IdentifiableApiResponse(permissionRequest, responseDocument);


        StepVerifier.create(provider.getConsumptionRecordStream())
                // When
                .then(() -> publisher.next(apiResponse))
                // Then
                .assertNext(consumptionRecord -> assertAll(
                        () -> assertEquals("conId", consumptionRecord.getConnectionId()),
                        () -> assertEquals("pId", consumptionRecord.getPermissionId()),
                        () -> assertEquals("dId", consumptionRecord.getDataNeedId()),
                        () -> assertEquals("PT15M", consumptionRecord.getMeteringInterval()),
                        () -> assertEquals(1, consumptionRecord.getConsumptionPoints().size())
                ))
                .thenCancel()
                .verify(Duration.ofSeconds(2));

        // cleanup
        provider.close();
    }
}
