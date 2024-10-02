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
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.scheduling.support.CronExpression;
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
    private static final String DATASOURCE_NAME = "TestDataSource";
    private Aggregator aggregator;
    private Set<String> wantedCodes;
    private AiidaRecord unwanted1;
    private AiidaRecord unwanted2;
    private AiidaRecord wanted1;
    private AiidaRecord wanted2;
    private Instant expiration;
    private CronExpression transmissionSchedule;
    @Mock
    private Flux<AiidaRecord> mockFlux;
    @Mock
    private AiidaRecordRepository mockRepository;
    private final HealthContributorRegistry healthContributorRegistry = new DefaultHealthContributorRegistry();

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));

        // add 10 minutes to the current time, as only records after the next scheduled cron timestamp are processed
        var instant = Instant.now().plusSeconds(600);

        wantedCodes = Set.of("1.8.0", "2.8.0");
        unwanted1 = AiidaRecordFactory.createRecord("1.7.0", instant, 10);
        unwanted2 = AiidaRecordFactory.createRecord("1.8.0", instant, 15);
        wanted1 = AiidaRecordFactory.createRecord("1.8.0", instant, 50);
        wanted2 = AiidaRecordFactory.createRecord("2.8.0", instant, 60);
        expiration = Instant.now().plusSeconds(300_000);
        transmissionSchedule = CronExpression.parse("* * * * * *");

        aggregator = new Aggregator(mockRepository, healthContributorRegistry);
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
        when(mockDataSource1.id()).thenReturn("1");
        when(mockDataSource1.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource1.start()).thenReturn(Flux.empty());
        var mockDataSource2 = mock(AiidaDataSource.class);
        when(mockDataSource2.id()).thenReturn("2");
        when(mockDataSource2.name()).thenReturn(DATASOURCE_NAME);
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
        when(mockDataSource1.id()).thenReturn("1");
        when(mockDataSource1.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource1.start()).thenReturn(publisher1.flux());
        var mockDataSource2 = mock(AiidaDataSource.class);
        when(mockDataSource2.id()).thenReturn("2");
        when(mockDataSource2.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource2.start()).thenReturn(publisher2.flux());


        aggregator.addNewAiidaDataSource(mockDataSource1);
        aggregator.addNewAiidaDataSource(mockDataSource2);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes, expiration, transmissionSchedule))
                                                .expectNextMatches(aiidaRecord -> aiidaRecord.code()
                                                                                             .equals("2.8.0") && ((IntegerAiidaRecord) aiidaRecord).value() == 60)
                                                .expectNextMatches(aiidaRecord -> aiidaRecord.code()
                                                                                             .equals("1.8.0") && ((IntegerAiidaRecord) aiidaRecord).value() == 50)
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
     * Tests that the Flux returned by {@link Aggregator#getFilteredFlux(Set, Instant, CronExpression)} only returns
     * {@link AiidaRecord}s that have been published after the returned Flux has been created.
     */
    @Test
    void getFilteredFlux_doesNotReturnDataPublishedBeforeSubscribed() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.id()).thenReturn("1");
        when(mockDataSource.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource.start()).thenReturn(publisher.flux());


        aggregator.addNewAiidaDataSource(mockDataSource);
        publisher.next(unwanted1);
        publisher.next(unwanted2);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes, expiration, transmissionSchedule))
                                                .expectNextMatches(aiidaRecord -> aiidaRecord.code()
                                                                                             .equals("2.8.0") && ((IntegerAiidaRecord) aiidaRecord).value() == 60)
                                                .expectNextMatches(aiidaRecord -> aiidaRecord.code()
                                                                                             .equals("1.8.0") && ((IntegerAiidaRecord) aiidaRecord).value() == 50)
                                                .thenCancel()
                                                .log()
                                                .verifyLater();


        publisher.next(wanted2);
        publisher.next(wanted1);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void getFilteredFlux_bufferRecordsByCron() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.start()).thenReturn(publisher.flux());

        var unwantedBeforeCron = AiidaRecordFactory.createRecord("1.8.0", Instant.now().minusSeconds(10), -50);

        aggregator.addNewAiidaDataSource(mockDataSource);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes, expiration, transmissionSchedule))
                                                .expectNextMatches(aiidaRecord -> aiidaRecord.code()
                                                                                             .equals("1.8.0") && ((IntegerAiidaRecord) aiidaRecord).value() == 50)
                                                .thenCancel()
                                                .log()
                                                .verifyLater();

        publisher.next(unwantedBeforeCron);
        publisher.next(wanted1);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenAiidaRecordFromDatasource_isSavedInDatabase() throws InterruptedException {
        TestPublisher<AiidaRecord> publisher1 = TestPublisher.create();
        var mockDataSource1 = mock(AiidaDataSource.class);
        when(mockDataSource1.id()).thenReturn("1");
        when(mockDataSource1.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource1.start()).thenReturn(publisher1.flux());


        TestPublisher<AiidaRecord> publisher2 = TestPublisher.create();
        var mockDataSource2 = mock(AiidaDataSource.class);
        when(mockDataSource2.id()).thenReturn("2");
        when(mockDataSource2.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource2.start()).thenReturn(publisher2.flux());


        aggregator.addNewAiidaDataSource(mockDataSource1);
        aggregator.addNewAiidaDataSource(mockDataSource2);

        publisher1.next(wanted1);
        publisher1.next(wanted2);
        publisher1.complete();

        publisher2.next(wanted1);
        publisher2.next(wanted2);
        publisher2.complete();

        Thread.sleep(200);

        verify(mockRepository, times(4)).save(any(AiidaRecord.class));
    }

    /**
     * Tests that the Flux correctly filters {@link AiidaRecord}s that have a timestamp after the permission's
     * expiration time.
     */
    @Test
    void givenDataWithTimestampAfterFluxFilterTime_fluxDoesNotPublish() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.id()).thenReturn("1");
        when(mockDataSource.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource.start()).thenReturn(publisher.flux());
        var atExpirationTime = AiidaRecordFactory.createRecord("1.7.0", expiration, 111);
        var afterExpirationTime = AiidaRecordFactory.createRecord("2.7.0", expiration.plusSeconds(10), 111);

        aggregator.addNewAiidaDataSource(mockDataSource);

        StepVerifier.create(aggregator.getFilteredFlux(wantedCodes, expiration, transmissionSchedule))
                    .then(() -> {
                        publisher.next(wanted1);
                        publisher.next(atExpirationTime);
                        publisher.next(wanted2);
                        publisher.next(afterExpirationTime);
                    })
                    .expectNextMatches(aiidaRecord -> aiidaRecord.code()
                                                                 .equals("1.8.0") && ((IntegerAiidaRecord) aiidaRecord).value() == 50)
                    .expectNextMatches(aiidaRecord -> aiidaRecord.code()
                                                                 .equals("2.8.0") && ((IntegerAiidaRecord) aiidaRecord).value() == 60)
                    .then(aggregator::close)
                    .expectComplete()
                    .verify();
    }

    @Test
    void verify_close_emitsCompleteSignalForFilteredFlux() {
        var stepVerifier1 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 1"), expiration, transmissionSchedule))
                                        .expectComplete()
                                        .verifyLater();

        var stepVerifier2 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 2"), expiration, transmissionSchedule))
                                        .expectComplete()
                                        .verifyLater();

        var stepVerifier3 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 1"), expiration, transmissionSchedule))
                                        .expectComplete()
                                        .verifyLater();


        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.id()).thenReturn("1");
        when(mockDataSource.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource.start()).thenReturn(Flux.empty());


        aggregator.addNewAiidaDataSource(mockDataSource);
        aggregator.close();

        stepVerifier1.verify(Duration.ofSeconds(2));
        stepVerifier2.verify(Duration.ofSeconds(2));
        stepVerifier3.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenErrorFromDataSource_errorIsLoggedWithDataSourceName() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();

        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.id()).thenReturn("1");
        when(mockDataSource.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource.start()).thenReturn(publisher.flux());


        aggregator.addNewAiidaDataSource(mockDataSource);

        publisher.error(new IOException("My expected exception"));

        TestUtils.verifyErrorLogStartsWith("Error from datasource %s".formatted(DATASOURCE_NAME),
                                           logCaptor,
                                           IOException.class);
    }
}
