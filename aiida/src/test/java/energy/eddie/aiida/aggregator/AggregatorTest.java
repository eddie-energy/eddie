package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.TestUtils;
import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
import energy.eddie.aiida.models.record.IntegerAiidaRecord;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregatorTest {
    private static final LogCaptor logCaptor = LogCaptor.forClass(Aggregator.class);
    private Aggregator aggregator;
    private Set<String> wantedCodes;
    private AiidaRecord unwanted1;
    private AiidaRecord unwanted2;
    private AiidaRecord wanted1;
    private AiidaRecord wanted2;
    @Mock
    private Flux<AiidaRecord> mockFlux;
    @Mock
    private AiidaRecordRepository mockRepository;

    @BeforeEach
    void setUp() {
        wantedCodes = Set.of("1.8.0", "2.8.0");
        unwanted1 = AiidaRecordFactory.createRecord("1.7.0", Instant.now(), 10);
        unwanted2 = AiidaRecordFactory.createRecord("1.8.0", Instant.now(), 15);
        wanted1 = AiidaRecordFactory.createRecord("1.8.0", Instant.now(), 50);
        wanted2 = AiidaRecordFactory.createRecord("2.8.0", Instant.now(), 60);

        aggregator = new Aggregator(mockRepository);
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
    }

    @Test
    void givenNewDataSource_addNewDataSource_subscribesToFluxAndCallsStart() {
        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.start()).thenReturn(mockFlux);

        aggregator.addNewAiidaDataSource(mockDataSource);

        verify(mockDataSource).start();
    }

    @Test
    void verify_close_callsCloseOnAllDataSources() {
        var mockDataSource1 = mock(AiidaDataSource.class);
        var mockDataSource2 = mock(AiidaDataSource.class);
        when(mockDataSource1.start()).thenReturn(Flux.empty());
        when(mockDataSource2.start()).thenReturn(Flux.empty());


        aggregator.addNewAiidaDataSource(mockDataSource1);
        aggregator.addNewAiidaDataSource(mockDataSource2);

        aggregator.close();

        verify(mockDataSource1).close();
        verify(mockDataSource2).close();
    }

    @Test
    void getFilteredFlux_filtersFluxFromDataSources() {
        TestPublisher<AiidaRecord> publisher1 = TestPublisher.create();
        TestPublisher<AiidaRecord> publisher2 = TestPublisher.create();
        var mockDataSource1 = mock(AiidaDataSource.class);
        var mockDataSource2 = mock(AiidaDataSource.class);
        when(mockDataSource1.start()).thenReturn(publisher1.flux());
        when(mockDataSource2.start()).thenReturn(publisher2.flux());


        aggregator.addNewAiidaDataSource(mockDataSource1);
        aggregator.addNewAiidaDataSource(mockDataSource2);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes))
                .expectNextMatches(aiidaRecord -> aiidaRecord.code().equals("2.8.0") && ((IntegerAiidaRecord) aiidaRecord).value() == 60)
                .expectNextMatches(aiidaRecord -> aiidaRecord.code().equals("1.8.0") && ((IntegerAiidaRecord) aiidaRecord).value() == 50)
                .thenCancel()   // Flux of datasource don't terminate except if .close() is called
                .verifyLater();


        // must not matter which datasource publishes the data
        publisher1.next(unwanted1);
        publisher1.next(wanted2);
        publisher2.next(wanted1);
        publisher2.next(unwanted2);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    /**
     * Tests that the Flux returned by {@link Aggregator#getFilteredFlux(Set)} only returns {@link AiidaRecord}s
     * that has been published after the returned Flux has been created.
     */
    @Test
    void getFilteredFlux_doesNotReturnDataPublishedBeforeSubscribed() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.start()).thenReturn(publisher.flux());


        aggregator.addNewAiidaDataSource(mockDataSource);
        publisher.next(unwanted1);
        publisher.next(unwanted2);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes))
                .expectNextMatches(aiidaRecord -> aiidaRecord.code().equals("2.8.0") && ((IntegerAiidaRecord) aiidaRecord).value() == 60)
                .expectNextMatches(aiidaRecord -> aiidaRecord.code().equals("1.8.0") && ((IntegerAiidaRecord) aiidaRecord).value() == 50)
                .thenCancel()
                .log()
                .verifyLater();


        publisher.next(wanted2);
        publisher.next(wanted1);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenAiidaRecordFromDatasource_isSavedInDatabase() {
        TestPublisher<AiidaRecord> publisher1 = TestPublisher.create();
        var mockDataSource1 = mock(AiidaDataSource.class);
        when(mockDataSource1.start()).thenReturn(publisher1.flux());

        TestPublisher<AiidaRecord> publisher2 = TestPublisher.create();
        var mockDataSource2 = mock(AiidaDataSource.class);
        when(mockDataSource2.start()).thenReturn(publisher2.flux());


        aggregator.addNewAiidaDataSource(mockDataSource1);
        aggregator.addNewAiidaDataSource(mockDataSource2);

        publisher1.next(wanted1);
        publisher1.next(wanted2);
        publisher2.next(wanted2);
        publisher2.next(wanted1);

        verify(mockRepository, times(4)).save(any(AiidaRecord.class));
    }

    @Test
    void verify_close_emitsCompleteSignalForFilteredFlux() {
        var stepVerifier1 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 1")))
                .expectComplete()
                .verifyLater();

        var stepVerifier2 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 2")))
                .expectComplete()
                .verifyLater();

        var stepVerifier3 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 1")))
                .expectComplete()
                .verifyLater();


        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.start()).thenReturn(Flux.empty());


        aggregator.addNewAiidaDataSource(mockDataSource);
        aggregator.close();

        stepVerifier1.verify(Duration.ofSeconds(2));
        stepVerifier2.verify(Duration.ofSeconds(2));
        stepVerifier3.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenErrorFromDataSource_errorIsLoggedWithDataSourceName() {
        String name = "Test DataSource";
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();

        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.start()).thenReturn(publisher.flux());
        when(mockDataSource.name()).thenReturn(name);


        aggregator.addNewAiidaDataSource(mockDataSource);

        publisher.error(new IOException("My expected exception"));

        TestUtils.verifyErrorLogStartsWith("Error from datasource %s".formatted(name), logCaptor, IOException.class);
    }
}
