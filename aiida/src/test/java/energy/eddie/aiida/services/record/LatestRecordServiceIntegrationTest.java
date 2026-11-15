// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.record;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import energy.eddie.aiida.dtos.record.LatestDataSourceRecordDto;
import energy.eddie.aiida.errors.record.LatestAiidaRecordNotFoundException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceIcon;
import energy.eddie.aiida.models.datasource.interval.simulation.SimulationDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaRecordValueDto;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static energy.eddie.api.agnostic.aiida.ObisCode.POSITIVE_ACTIVE_ENERGY;
import static energy.eddie.api.agnostic.aiida.ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Runs without a surrounding test transaction to mirror production, where {@code spring.jpa.open-in-view} is
 * disabled and lazy collections can only be initialized inside a service-level transaction.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create", "spring.jpa.open-in-view=false"})
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(LatestRecordService.class)
@ExtendWith(MockitoExtension.class)
@MockitoBean(types = {
        PermissionLatestRecordMap.class,
        InboundRecordService.class,
        PermissionRepository.class,
        InboundAggregator.class,
        AuthService.class
})
class LatestRecordServiceIntegrationTest {
    private static final String TIMESCALEDB_IMAGE = "timescale/timescaledb:latest-pg17";
    private static final String TIMESCALEDB_CREATE_AIIDA_DB_AND_EMQX_USER_FILE =
            "timescaledb/create-aiida-db-and-emqx-user.sql";
    private static final String TIMESCALEDB_CONTAINER_PATH =
            "/docker-entrypoint-initdb.d/create-aiida-db-and-emqx-user.sql";
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    static final PostgreSQLContainer timescale =
            new PostgreSQLContainer(
                    DockerImageName.parse(TIMESCALEDB_IMAGE)
                                   .asCompatibleSubstituteFor("postgres"))
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(TIMESCALEDB_CREATE_AIIDA_DB_AND_EMQX_USER_FILE),
                            TIMESCALEDB_CONTAINER_PATH
                    );
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    @Autowired
    private LatestRecordService latestRecordService;
    @Autowired
    private AiidaRecordRepository aiidaRecordRepository;
    @Autowired
    private DataSourceRepository dataSourceRepository;
    @Mock
    private SimulationDataSourceDto simulationDataSourceDto;

    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        when(simulationDataSourceDto.enabled()).thenReturn(true);
        when(simulationDataSourceDto.name()).thenReturn("SIM");
        when(simulationDataSourceDto.countryCode()).thenReturn("AT");
        when(simulationDataSourceDto.icon()).thenReturn(DataSourceIcon.ELECTRICITY);
        when(simulationDataSourceDto.asset()).thenReturn(AiidaAsset.SUBMETER);

        dataSource = dataSourceRepository.saveAndFlush(new SimulationDataSource(simulationDataSourceDto,
                                                                                UUID.randomUUID()));

        aiidaRecordRepository.saveAndFlush(recordWithValues(NOW.minusSeconds(10), "10", "100"));
        aiidaRecordRepository.saveAndFlush(recordWithValues(NOW.minusSeconds(5), "15", "150"));
        aiidaRecordRepository.saveAndFlush(recordWithValues(NOW, "20", "200"));
    }

    @AfterEach
    void tearDown() {
        aiidaRecordRepository.deleteAll();
        dataSourceRepository.deleteAll();
    }

    @Test
    void latestDataSourceRecord_withoutOpenSession_mapsLazyRecordValues() throws LatestAiidaRecordNotFoundException {
        var latest = latestRecordService.latestDataSourceRecord(dataSource.id());

        assertEquals(NOW, latest.timestamp());
        assertEquals(dataSource.id(), latest.dataSourceId());
        assertEquals("SIM", latest.name());
        assertEquals(List.of("20", "200"), values(latest));
    }

    @Test
    void latestDataSourceRecords_withoutOpenSession_mapsLazyRecordValuesNewestFirst() throws LatestAiidaRecordNotFoundException {
        var latest = latestRecordService.latestDataSourceRecords(dataSource.id(), 2);

        assertEquals(2, latest.size());
        assertEquals(NOW, latest.getFirst().timestamp());
        assertEquals(List.of("20", "200"), values(latest.getFirst()));
        assertEquals(NOW.minusSeconds(5), latest.get(1).timestamp());
        assertEquals(List.of("15", "150"), values(latest.get(1)));
    }

    private AiidaRecord recordWithValues(Instant timestamp, String energy, String power) {
        var aiidaRecord = new AiidaRecord(timestamp, dataSource, List.of(
                new AiidaRecordValue(POSITIVE_ACTIVE_ENERGY.toString(),
                                     POSITIVE_ACTIVE_ENERGY,
                                     energy,
                                     UnitOfMeasurement.KILO_WATT_HOUR,
                                     energy,
                                     UnitOfMeasurement.KILO_WATT_HOUR),
                new AiidaRecordValue(POSITIVE_ACTIVE_INSTANTANEOUS_POWER.toString(),
                                     POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                     power,
                                     UnitOfMeasurement.WATT,
                                     power,
                                     UnitOfMeasurement.WATT)));
        aiidaRecord.aiidaRecordValues().forEach(value -> value.setAiidaRecord(aiidaRecord));
        return aiidaRecord;
    }

    private static List<String> values(LatestDataSourceRecordDto latestRecord) {
        return latestRecord.aiidaRecordValues().stream().map(AiidaRecordValueDto::value).toList();
    }
}
