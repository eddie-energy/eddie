package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.utils.TestUtils;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.aiida.models.record.UnitOfMeasurement.KW;
import static energy.eddie.aiida.models.record.UnitOfMeasurement.KWH;
import static energy.eddie.aiida.utils.ObisCode.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregatorTest {
    private static final LogCaptor logCaptor = LogCaptor.forClass(Aggregator.class);
    private static final String DATASOURCE_NAME = "TestDataSource";
    private static final UUID dataSourceId1 = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID dataSourceId2 = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private final HealthContributorRegistry healthContributorRegistry = new DefaultHealthContributorRegistry();
    private Aggregator aggregator;
    private Set<String> wantedCodes;
    private AiidaRecord unwanted1;
    private AiidaRecord unwanted2;
    private AiidaRecord unwanted3;
    private AiidaRecord wanted1;
    private Instant expiration;
    private CronExpression transmissionSchedule;
    @Mock
    private Flux<AiidaRecord> mockFlux;
    @Mock
    private AiidaRecordRepository mockRepository;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));

        // add 10 minutes to the current time, as only records after the next scheduled cron timestamp are processed
        var instant = Instant.now().plusSeconds(600);

        wantedCodes = Set.of("1-0:1.8.0", "1-0:2.8.0");
        unwanted1 = new AiidaRecord(instant, "Test", dataSourceId1, List.of(
                new AiidaRecordValue("1-0:1.7.0", POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "10", KWH, "10", KWH)));
        unwanted2 = new AiidaRecord(instant, "Test", dataSourceId1, List.of(
                new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "15", KW, "10", KW)));
        unwanted3 = new AiidaRecord(instant, "Test", dataSourceId1, List.of(
                new AiidaRecordValue("1-0:2.8.0", NEGATIVE_ACTIVE_ENERGY, "60", KWH, "10", KWH)));
        wanted1 = new AiidaRecord(instant, "Test", dataSourceId1, List.of(
                new AiidaRecordValue("1-01.8.0", POSITIVE_ACTIVE_ENERGY, "50", KW, "10", KW)));
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
        UUID dataSourceId1 = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
        UUID dataSourceId2 = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");

        var mockDataSource1 = mock(AiidaDataSource.class);
        when(mockDataSource1.id()).thenReturn(dataSourceId1);
        when(mockDataSource1.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource1.start()).thenReturn(Flux.empty());
        var mockDataSource2 = mock(AiidaDataSource.class);
        when(mockDataSource2.id()).thenReturn(dataSourceId2);
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
        when(mockDataSource1.id()).thenReturn(dataSourceId1);
        when(mockDataSource1.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource1.start()).thenReturn(publisher1.flux());
        var mockDataSource2 = mock(AiidaDataSource.class);
        when(mockDataSource2.id()).thenReturn(dataSourceId2);
        when(mockDataSource2.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource2.start()).thenReturn(publisher2.flux());


        aggregator.addNewAiidaDataSource(mockDataSource1);
        aggregator.addNewAiidaDataSource(mockDataSource2);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes,
                                                                                   expiration,
                                                                                   transmissionSchedule))
                                                .expectNextMatches(aiidaRecord -> containsExpectedAiidaRecordValue(
                                                        aiidaRecord,
                                                        wanted1.aiidaRecordValue().getFirst()))
                                                .thenCancel()   // Flux of datasource doesn't terminate except if .close() is called
                                                .verifyLater();


        // must not matter which datasource publishes the data
        publisher1.next(unwanted3);
        publisher1.next(unwanted2);
        publisher2.next(wanted1);
        publisher2.next(unwanted1);

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
        when(mockDataSource.id()).thenReturn(dataSourceId1);
        when(mockDataSource.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource.start()).thenReturn(publisher.flux());

        aggregator.addNewAiidaDataSource(mockDataSource);
        publisher.next(unwanted3);
        publisher.next(unwanted2);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes,
                                                                                   expiration,
                                                                                   transmissionSchedule))
                                                .expectNextMatches(aiidaRecord -> containsExpectedAiidaRecordValue(
                                                        aiidaRecord,
                                                        wanted1.aiidaRecordValue().getFirst()))
                                                .thenCancel()   // Flux of datasource doesn't terminate except if .close() is called
                                                .verifyLater();


        publisher.next(wanted1);
        publisher.next(unwanted1);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void getFilteredFlux_bufferRecordsByCron() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.start()).thenReturn(publisher.flux());

        var unwantedBeforeCron = new AiidaRecord(Instant.now().minusSeconds(10), "Test", dataSourceId1,
                                                 List.of(new AiidaRecordValue("1-0:1.8.0",
                                                                              POSITIVE_ACTIVE_ENERGY,
                                                                              "50",
                                                                              KW,
                                                                              "10",
                                                                              KW)));

        aggregator.addNewAiidaDataSource(mockDataSource);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes,
                                                                                   expiration,
                                                                                   transmissionSchedule))
                                                .expectNextMatches(aiidaRecord -> containsExpectedAiidaRecordValue(
                                                        aiidaRecord,
                                                        wanted1.aiidaRecordValue().getFirst()))
                                                .thenCancel()
                                                .log()
                                                .verifyLater();

        publisher.next(unwantedBeforeCron);
        publisher.next(wanted1);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenAiidaRecordFromDatasource_isSavedInDatabase() {
        TestPublisher<AiidaRecord> publisher1 = TestPublisher.create();
        var mockDataSource1 = mock(AiidaDataSource.class);
        when(mockDataSource1.id()).thenReturn(dataSourceId1);
        when(mockDataSource1.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource1.start()).thenReturn(publisher1.flux());


        TestPublisher<AiidaRecord> publisher2 = TestPublisher.create();
        var mockDataSource2 = mock(AiidaDataSource.class);
        when(mockDataSource2.id()).thenReturn(dataSourceId2);
        when(mockDataSource2.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource2.start()).thenReturn(publisher2.flux());


        aggregator.addNewAiidaDataSource(mockDataSource1);
        aggregator.addNewAiidaDataSource(mockDataSource2);

        publisher1.next(wanted1);
        publisher1.complete();

        publisher2.next(wanted1);
        publisher2.complete();

        publisher1.flux().blockLast(Duration.of(200, ChronoUnit.MILLIS));
        publisher2.flux().blockLast(Duration.of(200, ChronoUnit.MILLIS));

        verify(mockRepository, times(2)).save(any(AiidaRecord.class));
    }

    /**
     * Tests that the Flux correctly filters {@link AiidaRecord}s that have a timestamp after the permission's
     * expiration time.
     */
    @Test
    void givenDataWithTimestampAfterFluxFilterTime_fluxDoesNotPublish() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.id()).thenReturn(dataSourceId1);
        when(mockDataSource.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource.start()).thenReturn(publisher.flux());

        var atExpirationTime = new AiidaRecord(expiration,
                                               "Test",
                                               dataSourceId1,
                                               List.of(new AiidaRecordValue("1-0:1.7.0",
                                                                            POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                                                            "111",
                                                                            KWH,
                                                                            "10",
                                                                            KWH)));
        var afterExpirationTime = new AiidaRecord(expiration.plusSeconds(10),
                                                  "Test",
                                                  dataSourceId1,
                                                  List.of(new AiidaRecordValue("1-0:2.7.0",
                                                                               NEGATIVE_ACTIVE_INSTANTANEOUS_POWER,
                                                                               "111",
                                                                               KWH,
                                                                               "10",
                                                                               KWH)));

        aggregator.addNewAiidaDataSource(mockDataSource);

        StepVerifier.create(aggregator.getFilteredFlux(wantedCodes, expiration, transmissionSchedule))
                    .then(() -> {
                        publisher.next(wanted1);
                        publisher.next(unwanted1);
                        publisher.next(atExpirationTime);
                        publisher.next(afterExpirationTime);
                    })
                    .expectNextMatches(aiidaRecord -> containsExpectedAiidaRecordValue(
                            aiidaRecord,
                            wanted1.aiidaRecordValue().getFirst()))
                    .then(aggregator::close)
                    .expectComplete()
                    .verify();
    }

    @Test
    void verify_close_emitsCompleteSignalForFilteredFlux() {
        var stepVerifier1 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 1"),
                                                                           expiration,
                                                                           transmissionSchedule))
                                        .expectComplete()
                                        .verifyLater();

        var stepVerifier2 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 2"),
                                                                           expiration,
                                                                           transmissionSchedule))
                                        .expectComplete()
                                        .verifyLater();

        var stepVerifier3 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 1"),
                                                                           expiration,
                                                                           transmissionSchedule))
                                        .expectComplete()
                                        .verifyLater();


        var mockDataSource = mock(AiidaDataSource.class);
        when(mockDataSource.id()).thenReturn(dataSourceId1);
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
        when(mockDataSource.id()).thenReturn(dataSourceId1);
        when(mockDataSource.name()).thenReturn(DATASOURCE_NAME);
        when(mockDataSource.start()).thenReturn(publisher.flux());


        aggregator.addNewAiidaDataSource(mockDataSource);

        publisher.error(new IOException("My expected exception"));

        TestUtils.verifyErrorLogStartsWith("Error from datasource %s".formatted(DATASOURCE_NAME),
                                           logCaptor,
                                           IOException.class);
    }

    private boolean containsExpectedAiidaRecordValue(AiidaRecord actual, AiidaRecordValue expectedValue) {
        System.out.println("actual: " + actual.aiidaRecordValue().getFirst().rawValue());
        System.out.println("expected: " + expectedValue.rawValue());
        return actual.aiidaRecordValue()
                     .stream()
                     .anyMatch(aiidaRecordValue ->
                                       aiidaRecordValue.dataTag().equals(expectedValue.dataTag())
                                       && aiidaRecordValue.rawValue().equals(expectedValue.rawValue())
                                       && actual.timestamp().isBefore(expiration));
    }
}
