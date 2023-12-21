package energy.eddie.regionconnector.at.eda.processing.v0_82;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.cim.validated_historical_data.v0_82.ValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.EddieValidatedHistoricalDataMarketDocumentPublisher;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.ValidatedHistoricalDataMarketDocumentDirector;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumptionRecordProcessorTest {

    @Test
    void getEddieValidatedHistoricalDataMarketDocumentStream_publishesConsumptionRecordsAsMarketDocument() throws InvalidMappingException {
        TestPublisher<ConsumptionRecord> testPublisher = TestPublisher.create();
        ConsumptionRecord consumptionRecord = new ConsumptionRecord();
        ValidatedHistoricalDataMarketDocument validatedHistoricalDataMarketDocument = new ValidatedHistoricalDataMarketDocument();

        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        when(edaAdapter.getConsumptionRecordStream()).thenReturn(testPublisher.flux());

        ValidatedHistoricalDataMarketDocumentDirector director = mock(ValidatedHistoricalDataMarketDocumentDirector.class);
        when(director.createValidatedHistoricalDataMarketDocument(consumptionRecord)).thenReturn(validatedHistoricalDataMarketDocument);

        EddieValidatedHistoricalDataMarketDocumentPublisher publisher = mock(EddieValidatedHistoricalDataMarketDocumentPublisher.class);
        when(publisher.emitForEachPermissionRequest(validatedHistoricalDataMarketDocument)).thenReturn(Flux.fromArray(new EddieValidatedHistoricalDataMarketDocument[]{
                mock(EddieValidatedHistoricalDataMarketDocument.class),
                mock(EddieValidatedHistoricalDataMarketDocument.class),
                mock(EddieValidatedHistoricalDataMarketDocument.class)
        }));

        ConsumptionRecordProcessor consumptionRecordProcessor = new ConsumptionRecordProcessor(director, publisher, edaAdapter);


        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(consumptionRecordProcessor.getEddieValidatedHistoricalDataMarketDocumentStream()))
                .then(() -> testPublisher.next(consumptionRecord))
                .expectNextCount(3)
                .then(testPublisher::complete)
                .verifyComplete();
    }

    @Test
    void getEddieValidatedHistoricalDataMarketDocumentStream_whenDirectorThrows_emitsNothing() throws InvalidMappingException {
        TestPublisher<ConsumptionRecord> testPublisher = TestPublisher.create();
        ConsumptionRecord consumptionRecord = new ConsumptionRecord();

        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        when(edaAdapter.getConsumptionRecordStream()).thenReturn(testPublisher.flux());

        ValidatedHistoricalDataMarketDocumentDirector director = mock(ValidatedHistoricalDataMarketDocumentDirector.class);
        when(director.createValidatedHistoricalDataMarketDocument(consumptionRecord)).thenThrow(new InvalidMappingException(""));

        EddieValidatedHistoricalDataMarketDocumentPublisher publisher = mock(EddieValidatedHistoricalDataMarketDocumentPublisher.class);

        ConsumptionRecordProcessor consumptionRecordProcessor = new ConsumptionRecordProcessor(director, publisher, edaAdapter);


        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(consumptionRecordProcessor.getEddieValidatedHistoricalDataMarketDocumentStream()))
                .then(() -> testPublisher.next(consumptionRecord))
                .expectNextCount(0)
                .then(testPublisher::complete)
                .verifyComplete();
    }

    @Test
    void close_emitsCompleteOnPublisher() throws Exception {
        // Given
        ValidatedHistoricalDataMarketDocumentDirector director = mock(ValidatedHistoricalDataMarketDocumentDirector.class);
        EddieValidatedHistoricalDataMarketDocumentPublisher publisher = mock(EddieValidatedHistoricalDataMarketDocumentPublisher.class);
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        when(edaAdapter.getConsumptionRecordStream()).thenReturn(Flux.empty());
        ConsumptionRecordProcessor processor = new ConsumptionRecordProcessor(director, publisher, edaAdapter);

        StepVerifier stepVerifier = StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(processor.getEddieValidatedHistoricalDataMarketDocumentStream()))
                .expectComplete()
                .verifyLater();

        // When
        processor.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }
}