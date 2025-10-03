package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.inbound.InboundAdapter;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.simulation.SimulationDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.aiida.utils.TestUtils;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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

import static energy.eddie.aiida.models.record.UnitOfMeasurement.KILO_WATT;
import static energy.eddie.aiida.models.record.UnitOfMeasurement.KILO_WATT_HOUR;
import static energy.eddie.aiida.utils.ObisCode.*;
import static org.mockito.Mockito.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
class AggregatorTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(Aggregator.class);
    private static final String DATASOURCE_NAME_1 = "test-1";
    private static final UUID DATA_SOURCE_ID_1 = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID DATA_SOURCE_ID_2 = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID DATA_SOURCE_ID_3 = UUID.fromString("6211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56607");
    private static final SimulationDataSource DATA_SOURCE_1 = mock(SimulationDataSource.class);
    private static final SimulationDataSource DATA_SOURCE_2 = mock(SimulationDataSource.class);
    private static final InboundDataSource INBOUND_DATA_SOURCE = mock(InboundDataSource.class);
    private final HealthContributorRegistry healthContributorRegistry = new DefaultHealthContributorRegistry();
    private Aggregator aggregator;
    private AiidaAsset wantedAsset;
    private Set<String> wantedCodes;
    private AiidaRecord unwanted1;
    private AiidaRecord unwanted2;
    private AiidaRecord unwanted3;
    private AiidaRecord unwanted4;
    private AiidaRecord wanted;
    private Instant expiration;
    private CronExpression transmissionSchedule;
    @Mock
    private Flux<AiidaRecord> mockFlux;
    @Mock
    private AiidaRecordRepository mockAiidaRecordRepository;
    @Mock
    private InboundRecordRepository mockInboundRecordRepository;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));

        // add 10 minutes to the current time, as only records after the next scheduled cron timestamp are processed
        var instant = Instant.now().plusSeconds(600);

        when(DATA_SOURCE_1.id()).thenReturn(DATA_SOURCE_ID_1);
        when(DATA_SOURCE_1.name()).thenReturn(DATASOURCE_NAME_1);
        when(DATA_SOURCE_2.id()).thenReturn(DATA_SOURCE_ID_2);
        when(INBOUND_DATA_SOURCE.id()).thenReturn(DATA_SOURCE_ID_3);

        wantedAsset = AiidaAsset.SUBMETER;
        wantedCodes = Set.of("1-0:1.8.0", "1-0:2.8.0");
        unwanted1 = new AiidaRecord(instant, wantedAsset, USER_ID, DATA_SOURCE_ID_1, List.of(
                new AiidaRecordValue("1-0:1.7.0",
                                     POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                     "10",
                                     KILO_WATT_HOUR,
                                     "10",
                                     KILO_WATT_HOUR)));
        unwanted2 = new AiidaRecord(instant, wantedAsset, USER_ID, DATA_SOURCE_ID_1, List.of(
                new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "15", KILO_WATT, "10", KILO_WATT)));
        unwanted3 = new AiidaRecord(instant, wantedAsset, USER_ID, DATA_SOURCE_ID_1, List.of(
                new AiidaRecordValue("1-0:2.8.0", NEGATIVE_ACTIVE_ENERGY, "60", KILO_WATT_HOUR, "10", KILO_WATT_HOUR)));
        unwanted4 = new AiidaRecord(instant, AiidaAsset.CONTROLLABLE_UNIT, USER_ID, DATA_SOURCE_ID_1, List.of(
                new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "50", KILO_WATT, "10", KILO_WATT)));
        wanted = new AiidaRecord(instant, wantedAsset, USER_ID, DATA_SOURCE_ID_1, List.of(
                new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "50", KILO_WATT, "10", KILO_WATT)));
        expiration = Instant.now().plusSeconds(300_000);
        transmissionSchedule = CronExpression.parse("* * * * * *");

        aggregator = new Aggregator(mockAiidaRecordRepository, mockInboundRecordRepository, healthContributorRegistry);
    }

    @AfterEach
    void tearDown() {
        LOG_CAPTOR.clearLogs();
    }

    @Test
    void givenNewDataSource_addNewDataSource_subscribesToFluxAndCallsStart() {
        var mockAdapter = mock(DataSourceAdapter.class);
        when(mockAdapter.dataSource()).thenReturn(DATA_SOURCE_1);
        when(mockAdapter.start()).thenReturn(mockFlux);

        aggregator.addNewDataSourceAdapter(mockAdapter);

        verify(mockAdapter).start();
    }

    @Test
    void verify_close_callsCloseOnAllDataSources() {
        var mockAdapter1 = mock(DataSourceAdapter.class);
        when(mockAdapter1.dataSource()).thenReturn(DATA_SOURCE_1);
        when(mockAdapter1.start()).thenReturn(Flux.empty());
        var mockAdapter2 = mock(DataSourceAdapter.class);
        when(mockAdapter2.dataSource()).thenReturn(DATA_SOURCE_2);
        when(mockAdapter2.start()).thenReturn(Flux.empty());


        aggregator.addNewDataSourceAdapter(mockAdapter1);
        aggregator.addNewDataSourceAdapter(mockAdapter2);

        aggregator.close();

        verify(mockAdapter1).close();
        verify(mockAdapter2).close();
    }

    @Test
    @Disabled("Disable till better aggregation method: GH-1307")
    void getFilteredFlux_filtersFluxFromDataSources() {
        var instant = Instant.now().plusSeconds(600);
        wanted = new AiidaRecord(instant, AiidaAsset.SUBMETER, USER_ID, DATA_SOURCE_ID_1, List.of(
                new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "50", KILO_WATT, "10", KILO_WATT)));
        unwanted2 = new AiidaRecord(instant, AiidaAsset.SUBMETER, USER_ID, DATA_SOURCE_ID_2, List.of(
                new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "15", KILO_WATT, "10", KILO_WATT)));

        TestPublisher<AiidaRecord> publisher1 = TestPublisher.create();
        TestPublisher<AiidaRecord> publisher2 = TestPublisher.create();
        var mockAdapter1 = mock(DataSourceAdapter.class);
        when(mockAdapter1.dataSource()).thenReturn(DATA_SOURCE_1);
        when(mockAdapter1.start()).thenReturn(publisher1.flux());
        var mockAdapter2 = mock(DataSourceAdapter.class);
        when(mockAdapter2.dataSource()).thenReturn(DATA_SOURCE_2);
        when(mockAdapter2.start()).thenReturn(publisher2.flux());

        aggregator.addNewDataSourceAdapter(mockAdapter1);
        aggregator.addNewDataSourceAdapter(mockAdapter2);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes,
                                                                                   wantedAsset,
                                                                                   expiration,
                                                                                   transmissionSchedule,
                                                                                   USER_ID,
                                                                                   DATA_SOURCE_ID_1))
                                                .expectNextMatches(aiidaRecord -> containsExpectedAiidaRecordValue(
                                                        aiidaRecord,
                                                        unwanted3.aiidaRecordValues().getFirst()))
                                                .thenCancel()   // Flux of datasource doesn't terminate except if .close() is called
                                                .verifyLater();


        // must not matter which datasource publishes the data
        publisher1.next(unwanted3);
        publisher1.next(unwanted4);
        publisher2.next(wanted);
        publisher2.next(unwanted1);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    /**
     * Tests that the Flux returned by {@link Aggregator#getFilteredFlux(Set, AiidaAsset, Instant, CronExpression, UUID, UUID)} only returns
     * {@link AiidaRecord}s that have been published after the returned Flux has been created.
     */
    @Test
    @Disabled("Disable till better aggregation method: GH-1307")
    void getFilteredFlux_doesNotReturnDataPublishedBeforeSubscribed() throws InterruptedException {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        var mockAdapter = mock(DataSourceAdapter.class);
        when(mockAdapter.dataSource()).thenReturn(DATA_SOURCE_1);
        when(mockAdapter.start()).thenReturn(publisher.flux());

        aggregator.addNewDataSourceAdapter(mockAdapter);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes,
                                                                                   wantedAsset,
                                                                                   expiration,
                                                                                   transmissionSchedule,
                                                                                   USER_ID,
                                                                                   DATA_SOURCE_ID_1))
                                                .expectNextMatches(aiidaRecord -> containsExpectedAiidaRecordValue(
                                                        aiidaRecord,
                                                        wanted.aiidaRecordValues().getFirst()))
                                                .thenCancel()
                                                .verifyLater();

        Thread.sleep(200);

        publisher.next(unwanted3);
        publisher.next(unwanted2);
        publisher.next(wanted);
        publisher.next(unwanted1);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void getFilteredFlux_bufferRecordsByCron() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        var mockAdapter = mock(DataSourceAdapter.class);
        when(mockAdapter.dataSource()).thenReturn(DATA_SOURCE_1);
        when(mockAdapter.start()).thenReturn(publisher.flux());

        var unwantedBeforeCron = new AiidaRecord(Instant.now().minusSeconds(10), wantedAsset, USER_ID, DATA_SOURCE_ID_1,
                                                 List.of(new AiidaRecordValue("1-0:1.8.0",
                                                                              POSITIVE_ACTIVE_ENERGY,
                                                                              "50",
                                                                              KILO_WATT,
                                                                              "10",
                                                                              KILO_WATT)));

        aggregator.addNewDataSourceAdapter(mockAdapter);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes,
                                                                                   wantedAsset,
                                                                                   expiration,
                                                                                   transmissionSchedule,
                                                                                   USER_ID,
                                                                                   DATA_SOURCE_ID_1))
                                                .expectNextMatches(aiidaRecord -> containsExpectedAiidaRecordValue(
                                                        aiidaRecord,
                                                        wanted.aiidaRecordValues().getFirst()))
                                                .thenCancel()
                                                .log()
                                                .verifyLater();

        publisher.next(unwantedBeforeCron);
        publisher.next(wanted);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenAiidaRecordFromDatasource_isSavedInDatabase() {
        TestPublisher<AiidaRecord> publisher1 = TestPublisher.create();
        var mockAdapter1 = Mockito.<DataSourceAdapter<DataSource>>mock();
        when(mockAdapter1.dataSource()).thenReturn(DATA_SOURCE_1);
        when(mockAdapter1.start()).thenReturn(publisher1.flux());


        TestPublisher<AiidaRecord> publisher2 = TestPublisher.create();
        var mockAdapter2 = Mockito.<DataSourceAdapter<DataSource>>mock();
        when(mockAdapter2.dataSource()).thenReturn(DATA_SOURCE_2);
        when(mockAdapter2.start()).thenReturn(publisher2.flux());

        aggregator.addNewDataSourceAdapter(mockAdapter1);
        aggregator.addNewDataSourceAdapter(mockAdapter2);

        publisher1.emit(wanted);
        publisher2.emit(wanted);

        publisher1.flux().blockLast(Duration.of(200, ChronoUnit.MILLIS));
        publisher2.flux().blockLast(Duration.of(200, ChronoUnit.MILLIS));

        await().atMost(Duration.ofSeconds(1))
               .untilAsserted(() -> verify(mockAiidaRecordRepository, times(2)).save(any(AiidaRecord.class)));
    }

    /**
     * Tests that the Flux correctly filters {@link AiidaRecord}s that have a timestamp after the permission's
     * expiration time.
     */
    @Test
    @Disabled("Disable till better aggregation method: GH-1307")
    void givenDataWithTimestampAfterFluxFilterTime_fluxDoesNotPublish() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        var mockAdapter = mock(DataSourceAdapter.class);
        when(mockAdapter.dataSource()).thenReturn(DATA_SOURCE_1);
        when(mockAdapter.start()).thenReturn(publisher.flux());

        var atExpirationTime = new AiidaRecord(expiration,
                                               wantedAsset,
                                               USER_ID,
                                               DATA_SOURCE_ID_1,
                                               List.of(new AiidaRecordValue("1-0:1.7.0",
                                                                            POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                                                            "111",
                                                                            KILO_WATT_HOUR,
                                                                            "10",
                                                                            KILO_WATT_HOUR)));
        var afterExpirationTime = new AiidaRecord(expiration.plusSeconds(10),
                                                  wantedAsset,
                                                  USER_ID,
                                                  DATA_SOURCE_ID_1,
                                                  List.of(new AiidaRecordValue("1-0:2.7.0",
                                                                               NEGATIVE_ACTIVE_INSTANTANEOUS_POWER,
                                                                               "111",
                                                                               KILO_WATT_HOUR,
                                                                               "10",
                                                                               KILO_WATT_HOUR)));

        aggregator.addNewDataSourceAdapter(mockAdapter);

        StepVerifier.create(aggregator.getFilteredFlux(wantedCodes,
                                                       wantedAsset,
                                                       expiration,
                                                       transmissionSchedule,
                                                       USER_ID,
                                                       DATA_SOURCE_ID_1))
                    .then(() -> {
                        publisher.next(wanted);
                        publisher.next(unwanted1);
                        publisher.next(atExpirationTime);
                        publisher.next(afterExpirationTime);
                    })
                    .expectNextMatches(aiidaRecord -> containsExpectedAiidaRecordValue(
                            aiidaRecord,
                            wanted.aiidaRecordValues().getFirst()))
                    .then(aggregator::close)
                    .expectComplete()
                    .verify();
    }

    @Test
    void verify_close_emitsCompleteSignalForFilteredFlux() {
        var stepVerifier1 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 1"),
                                                                           wantedAsset,
                                                                           expiration,
                                                                           transmissionSchedule,
                                                                           USER_ID,
                                                                           DATA_SOURCE_ID_1))
                                        .expectComplete()
                                        .verifyLater();

        var stepVerifier2 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 2"),
                                                                           wantedAsset,
                                                                           expiration,
                                                                           transmissionSchedule,
                                                                           USER_ID,
                                                                           DATA_SOURCE_ID_1))
                                        .expectComplete()
                                        .verifyLater();

        var stepVerifier3 = StepVerifier.create(aggregator.getFilteredFlux(Set.of("Some Test 1"),
                                                                           wantedAsset,
                                                                           expiration,
                                                                           transmissionSchedule,
                                                                           USER_ID,
                                                                           DATA_SOURCE_ID_1))
                                        .expectComplete()
                                        .verifyLater();


        var mockAdapter = mock(DataSourceAdapter.class);
        when(mockAdapter.dataSource()).thenReturn(DATA_SOURCE_1);
        when(mockAdapter.start()).thenReturn(Flux.empty());


        aggregator.addNewDataSourceAdapter(mockAdapter);
        aggregator.close();

        stepVerifier1.verify(Duration.ofSeconds(2));
        stepVerifier2.verify(Duration.ofSeconds(2));
        stepVerifier3.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenErrorFromDataSource_errorIsLoggedWithDataSourceName() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();

        var mockAdapter = mock(DataSourceAdapter.class);
        when(mockAdapter.dataSource()).thenReturn(DATA_SOURCE_1);
        when(mockAdapter.start()).thenReturn(publisher.flux());


        aggregator.addNewDataSourceAdapter(mockAdapter);

        publisher.error(new IOException("My expected exception"));

        TestUtils.verifyErrorLogStartsWith("Error from datasource %s".formatted(DATASOURCE_NAME_1),
                                           LOG_CAPTOR,
                                           IOException.class);
    }

    @Test
    void givenInboundDataSource_savedToInboundRepository() {
        TestPublisher<AiidaRecord> recordPublisher = TestPublisher.create();
        TestPublisher<InboundRecord> inboundPublisher = TestPublisher.create();

        var mockAdapter = mock(InboundAdapter.class);
        when(mockAdapter.dataSource()).thenReturn(INBOUND_DATA_SOURCE);
        when(mockAdapter.inboundRecordFlux()).thenReturn(inboundPublisher.flux());
        when(mockAdapter.start()).thenReturn(recordPublisher.flux());

        aggregator.addNewDataSourceAdapter(mockAdapter);

        inboundPublisher.next(new InboundRecord(Instant.now(), AiidaAsset.SUBMETER, USER_ID, DATA_SOURCE_ID_3, "Test"));
        inboundPublisher.complete();
        recordPublisher.complete();

        verify(mockInboundRecordRepository, times(1)).save(any(InboundRecord.class));
    }

    private boolean containsExpectedAiidaRecordValue(AiidaRecord actual, AiidaRecordValue expectedValue) {
        System.out.println("actual: " + actual.aiidaRecordValues().getFirst().rawValue());
        System.out.println("expected: " + expectedValue.rawValue());
        return actual.aiidaRecordValues()
                     .stream()
                     .anyMatch(aiidaRecordValue ->
                                       aiidaRecordValue.dataTag().equals(expectedValue.dataTag())
                                       && aiidaRecordValue.rawValue().equals(expectedValue.rawValue())
                                       && actual.timestamp().isBefore(expiration));
    }
}
