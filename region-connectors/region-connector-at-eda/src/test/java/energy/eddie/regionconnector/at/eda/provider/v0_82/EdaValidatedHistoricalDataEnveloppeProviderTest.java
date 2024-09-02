package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocumentComplexType;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.ValidatedHistoricalDataMarketDocumentDirector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdaValidatedHistoricalDataEnvelopeProviderTest {
    @Mock
    private ValidatedHistoricalDataMarketDocumentDirector director;

    @Test
    void getValidatedHistoricalDataMarketDocumentStream_publishesConsumptionRecordsAsMarketDocuments() throws Exception {
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
        var validatedHistoricalDataMarketDocument = new ValidatedHistoricalDataMarketDocumentComplexType();

        when(director.createValidatedHistoricalDataMarketDocument(consumptionRecord))
                .thenReturn(validatedHistoricalDataMarketDocument);

        try (var uut = new EdaValidatedHistoricalDataEnvelopeProvider(director, testPublisher.flux())) {
            StepVerifier.create(uut.getValidatedHistoricalDataMarketDocumentsStream())
                        .then(() -> {
                            testPublisher.next(new IdentifiableConsumptionRecord(consumptionRecord,
                                                                                 permissionRequests,
                                                                                 null,
                                                                                 null));
                            testPublisher.complete();
                        })
                        .assertNext(md -> assertAll(
                                () -> assertEquals(expectedString1,
                                                   md.getMessageDocumentHeader()
                                                     .getMessageDocumentHeaderMetaInformation()
                                                     .getPermissionid()),
                                () -> assertEquals(expectedString1,
                                                   md.getMessageDocumentHeader()
                                                     .getMessageDocumentHeaderMetaInformation()
                                                     .getConnectionid()),
                                () -> assertEquals(expectedString1,
                                                   md.getMessageDocumentHeader()
                                                     .getMessageDocumentHeaderMetaInformation()
                                                     .getDataNeedid()),
                                () -> assertEquals(validatedHistoricalDataMarketDocument,
                                                   md.getValidatedHistoricalDataMarketDocument())
                        ))
                        .assertNext(md -> assertAll(
                                () -> assertEquals(expectedString2,
                                                   md.getMessageDocumentHeader()
                                                     .getMessageDocumentHeaderMetaInformation()
                                                     .getPermissionid()),
                                () -> assertEquals(expectedString2,
                                                   md.getMessageDocumentHeader()
                                                     .getMessageDocumentHeaderMetaInformation()
                                                     .getConnectionid()),
                                () -> assertEquals(expectedString2,
                                                   md.getMessageDocumentHeader()
                                                     .getMessageDocumentHeaderMetaInformation()
                                                     .getDataNeedid()),
                                () -> assertEquals(validatedHistoricalDataMarketDocument,
                                                   md.getValidatedHistoricalDataMarketDocument())
                        ))
                        .assertNext(md -> assertAll(
                                () -> assertEquals(expectedString3,
                                                   md.getMessageDocumentHeader()
                                                     .getMessageDocumentHeaderMetaInformation()
                                                     .getPermissionid()),
                                () -> assertEquals(expectedString3,
                                                   md.getMessageDocumentHeader()
                                                     .getMessageDocumentHeaderMetaInformation()
                                                     .getConnectionid()),
                                () -> assertEquals(expectedString3,
                                                   md.getMessageDocumentHeader()
                                                     .getMessageDocumentHeaderMetaInformation()
                                                     .getDataNeedid()),
                                () -> assertEquals(validatedHistoricalDataMarketDocument,
                                                   md.getValidatedHistoricalDataMarketDocument())
                        ))
                        .expectComplete()
                        .verify(Duration.ofSeconds(2));
        }
    }

    private SimplePermissionRequest createPermissionRequest(String expected) {
        return new SimplePermissionRequest(expected, expected, expected);
    }

    @Test
    void getValidatedHistoricalDataMarketDocumentsStream_whenDirectorThrows_emitsNothing() throws Exception {
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        EdaConsumptionRecord consumptionRecord = new SimpleEdaConsumptionRecord();

        when(director.createValidatedHistoricalDataMarketDocument(consumptionRecord))
                .thenThrow(new InvalidMappingException(""));


        try (var uut = new EdaValidatedHistoricalDataEnvelopeProvider(
                director,
                testPublisher.flux())) {
            StepVerifier.create(uut.getValidatedHistoricalDataMarketDocumentsStream())
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
