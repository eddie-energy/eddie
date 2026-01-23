// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.adapters.datasource.inbound.InboundAdapter;
import energy.eddie.aiida.adapters.datasource.simulation.SimulationAdapter;
import energy.eddie.aiida.models.datasource.interval.simulation.SimulationDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.aiida.utils.TestUtils;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry;
import org.springframework.boot.health.registry.HealthContributorRegistry;
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

import static energy.eddie.api.agnostic.aiida.ObisCode.*;
import static energy.eddie.api.agnostic.aiida.UnitOfMeasurement.KILO_WATT;
import static energy.eddie.api.agnostic.aiida.UnitOfMeasurement.KILO_WATT_HOUR;
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
    @Mock
    private SimulationDataSource dataSource1;
    @Mock
    private SimulationDataSource dataSource2;
    @Mock
    private InboundDataSource inboundDataSource;
    private final HealthContributorRegistry healthContributorRegistry = new DefaultHealthContributorRegistry();
    private Aggregator aggregator;
    private AiidaAsset wantedAsset;
    private Set<ObisCode> wantedCodes;
    private AiidaRecord unwanted1;
    private AiidaRecord unwanted2;
    private AiidaRecord unwanted3;
    private AiidaRecord wanted;
    private Instant expiration;
    private CronExpression transmissionSchedule;
    @Mock
    private Flux<AiidaRecord> mockFlux;
    @Mock
    private SimulationAdapter mockAdapter1;
    @Mock
    private SimulationAdapter mockAdapter2;
    @Mock
    private InboundAdapter inboundAdapter;
    @Mock
    private AiidaRecordRepository mockAiidaRecordRepository;
    @Mock
    private InboundRecordRepository mockInboundRecordRepository;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));

        // add 10 minutes to the current time, as only records after the next scheduled cron timestamp are processed
        var instant = Instant.now().plusSeconds(600);

        // prevent stub validation since setting these for every test becomes tedious
        Mockito.lenient().when(dataSource1.id()).thenReturn(DATA_SOURCE_ID_1);
        Mockito.lenient().when(dataSource1.name()).thenReturn(DATASOURCE_NAME_1);
        Mockito.lenient().when(dataSource2.id()).thenReturn(DATA_SOURCE_ID_2);
        Mockito.lenient().when(inboundDataSource.id()).thenReturn(DATA_SOURCE_ID_3);
        Mockito.lenient().when(mockAdapter1.dataSource()).thenReturn(dataSource1);
        Mockito.lenient().when(mockAdapter2.dataSource()).thenReturn(dataSource2);

        wantedAsset = AiidaAsset.SUBMETER;
        wantedCodes = Set.of(POSITIVE_ACTIVE_ENERGY, NEGATIVE_ACTIVE_ENERGY);
        unwanted1 = new AiidaRecord(instant, wantedAsset, USER_ID, DATA_SOURCE_ID_1, List.of(
                new AiidaRecordValue("1-0:1.7.0",
                                     POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                     "10",
                                     KILO_WATT_HOUR,
                                     "10",
                                     KILO_WATT_HOUR)));
        unwanted2 = new AiidaRecord(instant, wantedAsset, USER_ID, DATA_SOURCE_ID_1, List.of(
                new AiidaRecordValue(NEGATIVE_ACTIVE_ENERGY.toString(), NEGATIVE_ACTIVE_ENERGY, "60", KILO_WATT_HOUR, "10", KILO_WATT_HOUR)));
        unwanted3 = new AiidaRecord(instant, AiidaAsset.CONTROLLABLE_UNIT, USER_ID, DATA_SOURCE_ID_1, List.of(
                new AiidaRecordValue(POSITIVE_ACTIVE_ENERGY.toString(), POSITIVE_ACTIVE_ENERGY, "50", KILO_WATT, "10", KILO_WATT)));
        wanted = new AiidaRecord(instant, wantedAsset, USER_ID, DATA_SOURCE_ID_1, List.of(
                new AiidaRecordValue(POSITIVE_ACTIVE_ENERGY.toString(), POSITIVE_ACTIVE_ENERGY, "50", KILO_WATT, "10", KILO_WATT)));
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
        when(mockAdapter1.start()).thenReturn(mockFlux);

        aggregator.addNewDataSourceAdapter(mockAdapter1);

        verify(mockAdapter1).start();
    }

    @Test
    void verify_close_callsCloseOnAllDataSources() {
        when(mockAdapter1.start()).thenReturn(Flux.empty());
        when(mockAdapter2.start()).thenReturn(Flux.empty());

        aggregator.addNewDataSourceAdapter(mockAdapter1);
        aggregator.addNewDataSourceAdapter(mockAdapter2);

        aggregator.close();

        verify(mockAdapter1).close();
        verify(mockAdapter2).close();
    }

    @Test
    void getFilteredFlux_filtersFluxFromDataSources() {
        var instant = Instant.now().plusSeconds(600);
        wanted = new AiidaRecord(instant, AiidaAsset.SUBMETER, USER_ID, DATA_SOURCE_ID_1, List.of(
                new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "50", KILO_WATT, "10", KILO_WATT)));
        unwanted2 = new AiidaRecord(instant, AiidaAsset.SUBMETER, USER_ID, DATA_SOURCE_ID_2, List.of(
                new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "15", KILO_WATT, "10", KILO_WATT)));

        TestPublisher<AiidaRecord> publisher1 = TestPublisher.create();
        TestPublisher<AiidaRecord> publisher2 = TestPublisher.create();
        when(mockAdapter1.start()).thenReturn(publisher1.flux());
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
                                                        wanted.aiidaRecordValues().getFirst()))
                                                .thenCancel()   // Flux of datasource doesn't terminate except if .close() is called
                                                .verifyLater();


        // must not matter which datasource publishes the data
        publisher1.next(unwanted2);
        publisher1.next(unwanted3);
        publisher2.next(wanted);
        publisher2.next(unwanted1);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void getFilteredFlux_bufferRecordsByCron() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        when(mockAdapter1.start()).thenReturn(publisher.flux());

        var unwantedBeforeCron = new AiidaRecord(Instant.now().minusSeconds(10), wantedAsset, USER_ID, DATA_SOURCE_ID_1,
                                                 List.of(new AiidaRecordValue("1-0:1.8.0",
                                                                              POSITIVE_ACTIVE_ENERGY,
                                                                              "50",
                                                                              KILO_WATT,
                                                                              "10",
                                                                              KILO_WATT)));

        aggregator.addNewDataSourceAdapter(mockAdapter1);

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
    void getFilteredFlux_mergeRecordsByRawTag() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        when(mockAdapter1.start()).thenReturn(publisher.flux());

        var record1 = new AiidaRecord(Instant.now().plusSeconds(600), wantedAsset, USER_ID, DATA_SOURCE_ID_1,
                                      List.of(
                                              new AiidaRecordValue("1-0:1.8.0",
                                                                   POSITIVE_ACTIVE_ENERGY,
                                                                   "50",
                                                                   KILO_WATT,
                                                                   "10",
                                                                   KILO_WATT),
                                              new AiidaRecordValue("1-0:2.8.0",
                                                                   POSITIVE_ACTIVE_ENERGY,
                                                                   "55",
                                                                   KILO_WATT,
                                                                   "10",
                                                                   KILO_WATT)
                                      ));
        var record2 = new AiidaRecord(Instant.now().plusSeconds(600), wantedAsset, USER_ID, DATA_SOURCE_ID_1,
                                      List.of(
                                              new AiidaRecordValue("1-0:2.8.0",
                                                                   POSITIVE_ACTIVE_ENERGY,
                                                                   "60",
                                                                   KILO_WATT,
                                                                   "12",
                                                                   KILO_WATT)
                                      ));

        aggregator.addNewDataSourceAdapter(mockAdapter1);

        StepVerifier stepVerifier = StepVerifier.create(aggregator.getFilteredFlux(wantedCodes,
                                                                                   wantedAsset,
                                                                                   expiration,
                                                                                   transmissionSchedule,
                                                                                   USER_ID,
                                                                                   DATA_SOURCE_ID_1))
                                                .expectNextMatches(aiidaRecord ->
                                                                           containsExpectedAiidaRecordValue(
                                                                                   aiidaRecord,
                                                                                   record2.aiidaRecordValues()
                                                                                          .getFirst())
                                                                           && containsExpectedAiidaRecordValue(
                                                                                   aiidaRecord,
                                                                                   record1.aiidaRecordValues()
                                                                                          .getFirst())
                                                )
                                                .thenCancel()
                                                .log()
                                                .verifyLater();

        publisher.next(record1);
        publisher.next(record2);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenAiidaRecordFromDatasource_isSavedInDatabase() {
        TestPublisher<AiidaRecord> publisher1 = TestPublisher.create();
        when(mockAdapter1.start()).thenReturn(publisher1.flux());


        TestPublisher<AiidaRecord> publisher2 = TestPublisher.create();
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
    void givenDataWithTimestampAfterFluxFilterTime_fluxDoesNotPublish() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();
        when(mockAdapter1.start()).thenReturn(publisher.flux());

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

        aggregator.addNewDataSourceAdapter(mockAdapter1);

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
        var stepVerifier1 = StepVerifier.create(aggregator.getFilteredFlux(Set.of(POSITIVE_ACTIVE_ENERGY_IN_PHASE_L1),
                                                                           wantedAsset,
                                                                           expiration,
                                                                           transmissionSchedule,
                                                                           USER_ID,
                                                                           DATA_SOURCE_ID_1))
                                        .expectComplete()
                                        .verifyLater();

        var stepVerifier2 = StepVerifier.create(aggregator.getFilteredFlux(Set.of(POSITIVE_ACTIVE_ENERGY_IN_PHASE_L2),
                                                                           wantedAsset,
                                                                           expiration,
                                                                           transmissionSchedule,
                                                                           USER_ID,
                                                                           DATA_SOURCE_ID_1))
                                        .expectComplete()
                                        .verifyLater();

        var stepVerifier3 = StepVerifier.create(aggregator.getFilteredFlux(Set.of(POSITIVE_ACTIVE_ENERGY_IN_PHASE_L3),
                                                                           wantedAsset,
                                                                           expiration,
                                                                           transmissionSchedule,
                                                                           USER_ID,
                                                                           DATA_SOURCE_ID_1))
                                        .expectComplete()
                                        .verifyLater();


        when(mockAdapter1.start()).thenReturn(Flux.empty());


        aggregator.addNewDataSourceAdapter(mockAdapter1);
        aggregator.close();

        stepVerifier1.verify(Duration.ofSeconds(2));
        stepVerifier2.verify(Duration.ofSeconds(2));
        stepVerifier3.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenErrorFromDataSource_errorIsLoggedWithDataSourceName() {
        TestPublisher<AiidaRecord> publisher = TestPublisher.create();

        when(mockAdapter1.dataSource()).thenReturn(dataSource1);
        when(mockAdapter1.start()).thenReturn(publisher.flux());


        aggregator.addNewDataSourceAdapter(mockAdapter1);

        publisher.error(new IOException("My expected exception"));

        TestUtils.verifyErrorLogStartsWith("Error from datasource %s".formatted(DATASOURCE_NAME_1),
                                           LOG_CAPTOR,
                                           IOException.class);
    }

    @Test
    void givenInboundDataSource_savedToInboundRepository() {
        TestPublisher<AiidaRecord> recordPublisher = TestPublisher.create();
        TestPublisher<InboundRecord> inboundPublisher = TestPublisher.create();

        when(inboundAdapter.dataSource()).thenReturn(inboundDataSource);
        when(inboundAdapter.inboundRecordFlux()).thenReturn(inboundPublisher.flux());
        when(inboundAdapter.start()).thenReturn(recordPublisher.flux());

        aggregator.addNewDataSourceAdapter(inboundAdapter);

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
