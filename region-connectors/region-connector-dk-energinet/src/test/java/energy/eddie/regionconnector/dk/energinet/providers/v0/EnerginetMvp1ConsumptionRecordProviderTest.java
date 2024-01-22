package energy.eddie.regionconnector.dk.energinet.providers.v0;

import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

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
        var apiResponse = new IdentifiableApiResponse("pId", "conId", "dId", List.of(responseDocument));


        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(provider.getConsumptionRecordStream()))
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