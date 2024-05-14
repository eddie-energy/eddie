package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.ValidatedHistoricalDataMarketDocumentDirector;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
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
        EdaConsumptionRecord consumptionRecord = new SimpleEdaConsumptionRecord();
        ValidatedHistoricalDataMarketDocument validatedHistoricalDataMarketDocument = new ValidatedHistoricalDataMarketDocument();

        ValidatedHistoricalDataMarketDocumentDirector director = mock(ValidatedHistoricalDataMarketDocumentDirector.class);
        when(director.createValidatedHistoricalDataMarketDocument(consumptionRecord)).thenReturn(
                validatedHistoricalDataMarketDocument);

        try (EdaEddieValidatedHistoricalDataMarketDocumentProvider uut = new EdaEddieValidatedHistoricalDataMarketDocumentProvider(
                director,
                testPublisher.flux())) {
            StepVerifier.create(uut.getEddieValidatedHistoricalDataMarketDocumentStream())
                        .then(() -> {
                            testPublisher.next(new IdentifiableConsumptionRecord(consumptionRecord,
                                                                                 permissionRequests,
                                                                                 null,
                                                                                 null));
                            testPublisher.complete();
                        })
                        .assertNext(md -> assertAll(
                                () -> assertEquals(expectedString1, md.permissionId()),
                                () -> assertEquals(expectedString1, md.connectionId()),
                                () -> assertEquals(expectedString1, md.dataNeedId()),
                                () -> assertEquals(validatedHistoricalDataMarketDocument, md.marketDocument())
                        ))
                        .assertNext(md -> assertAll(
                                () -> assertEquals(expectedString2, md.permissionId()),
                                () -> assertEquals(expectedString2, md.connectionId()),
                                () -> assertEquals(expectedString2, md.dataNeedId()),
                                () -> assertEquals(validatedHistoricalDataMarketDocument, md.marketDocument())
                        ))
                        .assertNext(md -> assertAll(
                                () -> assertEquals(expectedString3, md.permissionId()),
                                () -> assertEquals(expectedString3, md.connectionId()),
                                () -> assertEquals(expectedString3, md.dataNeedId()),
                                () -> assertEquals(validatedHistoricalDataMarketDocument, md.marketDocument())
                        ))
                        .expectComplete()
                        .verify(Duration.ofSeconds(2));
        }
    }

    private SimplePermissionRequest createPermissionRequest(String expected) {
        return new SimplePermissionRequest(expected, expected, expected);
    }

    @Test
    void getEddieValidatedHistoricalDataMarketDocumentStream_whenDirectorThrows_emitsNothing() throws Exception {
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        EdaConsumptionRecord consumptionRecord = new SimpleEdaConsumptionRecord();

        ValidatedHistoricalDataMarketDocumentDirector director = mock(ValidatedHistoricalDataMarketDocumentDirector.class);
        when(director.createValidatedHistoricalDataMarketDocument(consumptionRecord))
                .thenThrow(new InvalidMappingException(""));


        try (EdaEddieValidatedHistoricalDataMarketDocumentProvider uut = new EdaEddieValidatedHistoricalDataMarketDocumentProvider(
                director,
                testPublisher.flux())) {
            StepVerifier.create(uut.getEddieValidatedHistoricalDataMarketDocumentStream())
                        .then(() -> {
                            testPublisher.next(new IdentifiableConsumptionRecord(consumptionRecord,
                                                                                 List.of(),
                                                                                 null,
                                                                                 null));
                            testPublisher.complete();
                        })
                        .expectNextCount(0)
                        .verifyComplete();
        }
    }
}
