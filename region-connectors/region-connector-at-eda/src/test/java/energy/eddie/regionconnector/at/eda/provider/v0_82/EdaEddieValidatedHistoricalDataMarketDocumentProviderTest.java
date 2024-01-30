package energy.eddie.regionconnector.at.eda.provider.v0_82;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.ValidatedHistoricalDataMarketDocumentDirector;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EdaEddieValidatedHistoricalDataMarketDocumentProviderTest {

    @Test
    void getEddieValidatedHistoricalDataMarketDocumentStream_publishesConsumptionRecordsAsMarketDocument() throws Exception {
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        String expectedString1 = "expectedString1";
        String expectedString2 = "expectedString2";
        String expectedString3 = "expectedString3";
        List<AtPermissionRequest> permissionRequests = List.of(
                createPermissionRequest(expectedString1),
                createPermissionRequest(expectedString2),
                createPermissionRequest(expectedString3)
        );
        ConsumptionRecord consumptionRecord = new ConsumptionRecord();
        ValidatedHistoricalDataMarketDocument validatedHistoricalDataMarketDocument = new ValidatedHistoricalDataMarketDocument();

        ValidatedHistoricalDataMarketDocumentDirector director = mock(ValidatedHistoricalDataMarketDocumentDirector.class);
        when(director.createValidatedHistoricalDataMarketDocument(consumptionRecord)).thenReturn(validatedHistoricalDataMarketDocument);

        try (EdaEddieValidatedHistoricalDataMarketDocumentProvider uut = new EdaEddieValidatedHistoricalDataMarketDocumentProvider(director, testPublisher.flux())) {
            StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(uut.getEddieValidatedHistoricalDataMarketDocumentStream()))
                    .then(() -> {
                        testPublisher.next(new IdentifiableConsumptionRecord(consumptionRecord, permissionRequests));
                        testPublisher.complete();
                    })
                    .assertNext(cr -> {
                        assertThat(cr.permissionId()).hasValue(expectedString1);
                        assertThat(cr.connectionId()).hasValue(expectedString1);
                        assertThat(cr.dataNeedId()).hasValue(expectedString1);
                        assertEquals(validatedHistoricalDataMarketDocument, cr.marketDocument());
                    })
                    .assertNext(cr -> {
                        assertThat(cr.permissionId()).hasValue(expectedString2);
                        assertThat(cr.connectionId()).hasValue(expectedString2);
                        assertThat(cr.dataNeedId()).hasValue(expectedString2);
                        assertEquals(validatedHistoricalDataMarketDocument, cr.marketDocument());
                    })
                    .assertNext(cr -> {
                        assertThat(cr.permissionId()).hasValue(expectedString3);
                        assertThat(cr.connectionId()).hasValue(expectedString3);
                        assertThat(cr.dataNeedId()).hasValue(expectedString3);
                        assertEquals(validatedHistoricalDataMarketDocument, cr.marketDocument());
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
        }
    }

    @Test
    void getEddieValidatedHistoricalDataMarketDocumentStream_whenDirectorThrows_emitsNothing() throws Exception {
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        ConsumptionRecord consumptionRecord = new ConsumptionRecord();

        ValidatedHistoricalDataMarketDocumentDirector director = mock(ValidatedHistoricalDataMarketDocumentDirector.class);
        when(director.createValidatedHistoricalDataMarketDocument(consumptionRecord)).thenThrow(new InvalidMappingException(""));


        try (EdaEddieValidatedHistoricalDataMarketDocumentProvider uut = new EdaEddieValidatedHistoricalDataMarketDocumentProvider(director, testPublisher.flux())) {
            StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(uut.getEddieValidatedHistoricalDataMarketDocumentStream()))
                    .then(() -> {
                        testPublisher.next(new IdentifiableConsumptionRecord(consumptionRecord, List.of()));
                        testPublisher.complete();
                    })
                    .expectNextCount(0)
                    .verifyComplete();
        }
    }

    private SimplePermissionRequest createPermissionRequest(String expected) {
        return new SimplePermissionRequest(expected, expected, expected);
    }
}