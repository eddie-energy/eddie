// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create"})
@Testcontainers
class ConnectionLimitRepositoryIntegrationTest {
    private static final String TIMESCALEDB_IMAGE = "timescale/timescaledb:latest-pg17";
    private static final String TIMESCALEDB_CREATE_AIIDA_DB_AND_EMQX_USER_FILE = "timescaledb/create-aiida-db-and-emqx-user.sql";
    private static final String TIMESCALEDB_CONTAINER_PATH = "/docker-entrypoint-initdb.d/create-aiida-db-and-emqx-user.sql";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer timescale =
            new PostgreSQLContainer(DockerImageName.parse(TIMESCALEDB_IMAGE)
                                                   .asCompatibleSubstituteFor("postgres"))
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(TIMESCALEDB_CREATE_AIIDA_DB_AND_EMQX_USER_FILE),
                            TIMESCALEDB_CONTAINER_PATH
                    );

    private static final UUID USER_A = UUID.fromString("b8fac77d-75d9-4ebe-9f3d-b64ffb43ea00");
    private static final UUID USER_B = UUID.fromString("e4b6d77a-1d35-4bf2-bc2e-eaf0f5a5757d");
    private static final UUID PERMISSION_A1 = UUID.fromString("c7bca673-ef90-4f47-a6f6-42d0b47bb109");
    private static final UUID PERMISSION_A2 = UUID.fromString("7c981f8f-a3f0-4c59-aa43-185958758b50");
    private static final UUID PERMISSION_A3 = UUID.fromString("5298bfa2-fb44-4f84-b95f-c2a5af76844d");
    private static final UUID PERMISSION_B1 = UUID.fromString("752f0e41-0cea-4a3e-a1ee-8f12f27f7d8e");
    private static final UUID DATA_NEED_ID = UUID.fromString("f6d2054e-174d-4637-ae8b-cfdb205f462a");
    private static final String METER_1 = "meter-1";
    private static final String METER_2 = "meter-2";

    @Autowired
    private ConnectionLimitRepository connectionLimitRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        saveDataNeed();
        savePermission(PERMISSION_A1, USER_A);
        savePermission(PERMISSION_A2, USER_A);
        savePermission(PERMISSION_A3, USER_A);
        savePermission(PERMISSION_B1, USER_B);

        saveLimit(PERMISSION_A1, METER_1, "2026-07-10T09:45:00Z", "2026-07-10T10:00:00Z");
        saveLimit(PERMISSION_A1, METER_1, "2026-07-10T10:00:00Z", "2026-07-10T10:15:00Z");
        saveLimit(PERMISSION_A1, METER_1, "2026-07-10T10:10:00Z", "2026-07-10T10:25:00Z");
        saveLimit(PERMISSION_A1, null, "2026-07-10T10:15:00Z", "2026-07-10T10:30:00Z");
        saveLimit(PERMISSION_A1, METER_2, "2026-07-10T10:30:00Z", "2026-07-10T10:45:00Z");
        saveLimit(PERMISSION_A2, METER_1, "2026-07-10T10:00:00Z", "2026-07-10T10:15:00Z");
        saveLimit(PERMISSION_A2, METER_1, "2026-07-10T10:25:00Z", "2026-07-10T10:40:00Z");
        saveLimit(PERMISSION_B1, METER_1, "2026-07-10T10:00:00Z", "2026-07-10T10:15:00Z");
    }

    @Test
    void findByUserIdAndFiltersFromTo_withUserFilter_returnsOnlyUserRows() {
        var userPermissions = Set.of(PERMISSION_A1, PERMISSION_A2);
        var result = connectionLimitRepository.findByUserIdAndFiltersFromTo(
                USER_A,
                null,
                null,
                Instant.parse("2026-07-10T10:00:00Z"),
                null,
                Pageable.unpaged()
        );

        assertEquals(6, result.size());
        for (var limit : result) {
            assertTrue(userPermissions.contains(limit.permissionId()));
        }
    }

    @Test
    void findByUserIdAndFiltersFromTo_withMeterFilter_returnsOnlyMeterRows() {
        var result = connectionLimitRepository.findByUserIdAndFiltersFromTo(
                USER_A,
                null,
                METER_1,
                Instant.parse("2026-07-10T10:00:00Z"),
                Instant.parse("2026-07-10T10:20:00Z"),
                Pageable.unpaged()
        );

        assertEquals(3, result.size());
        for (var limit : result) {
            assertEquals(METER_1, limit.meterId());
        }
    }

    @Test
    void findByUserIdAndFiltersFromTo_withPermissionFilter_returnsOnlyPermissionRows() {
        var result = connectionLimitRepository.findByUserIdAndFiltersFromTo(
                USER_A,
                PERMISSION_A1,
                null,
                Instant.parse("2026-07-10T10:00:00Z"),
                Instant.parse("2026-07-10T10:20:00Z"),
                Pageable.unpaged()
        );

        assertEquals(3, result.size());
        for (var limit : result) {
            assertEquals(PERMISSION_A1, limit.permissionId());
        }
    }

    @Test
    void findByUserIdAndFiltersFromTo_withFromTo_returnsOverlappingIntervals() {
        var from = Instant.parse("2026-07-10T10:05:00Z");
        var to = Instant.parse("2026-07-10T10:20:00Z");
        var result = connectionLimitRepository.findByUserIdAndFiltersFromTo(
                USER_A,
                null,
                null,
                from,
                to,
                Pageable.unpaged()
        );

        assertEquals(4, result.size());
        for (var limit : result) {
            assertTrue(limit.intervalEnd().isAfter(from) && !limit.intervalStart().isAfter(to));
        }
    }

    @Test
    void findByUserIdAndFiltersFromTo_withMismatchedMeter_returnsEmpty() {
        var result = connectionLimitRepository.findByUserIdAndFiltersFromTo(
                USER_A,
                null,
                "unknown-meter",
                Instant.parse("2026-07-10T10:00:00Z"),
                null,
                Pageable.unpaged()
        );

        assertEquals(0, result.size());
    }

    @Test
    void findByUserIdAndFiltersFromTo_withFromEqualIntervalEnd_excludesIntervalEndAtStart() {
        var result = connectionLimitRepository.findByUserIdAndFiltersFromTo(
                USER_A,
                PERMISSION_A1,
                METER_1,
                Instant.parse("2026-07-10T10:00:00Z"),
                Instant.parse("2026-07-10T10:05:00Z"),
                Pageable.unpaged()
        );

        // Should only hit the 10:00 to 10:15 and not 9:45 to 10:00 limit
        assertEquals(1, result.size());
    }

    @Test
    void findEffectiveByUserIdAndFiltersFromTo_withOverlaps_returnsNewestCreatedAtTimeline() {
        saveLimit(PERMISSION_A3,
                  METER_1,
                  "2026-07-10T10:00:00Z",
                  "2026-07-10T10:15:00Z",
                  "2.0",
                  "8.0",
                  "2026-07-10T10:00:00Z");
        saveLimit(PERMISSION_A3,
                  METER_1,
                  "2026-07-10T10:10:00Z",
                  "2026-07-10T10:25:00Z",
                  "3.0",
                  "9.0",
                  "2026-07-10T10:05:00Z");
        saveLimit(PERMISSION_A3,
                  METER_1,
                  "2026-07-10T10:25:00Z",
                  "2026-07-10T10:30:00Z",
                  "1.0",
                  "7.0",
                  "2026-07-10T10:01:00Z");

        var result = connectionLimitRepository.findEffectiveByUserIdAndFiltersFromTo(USER_A,
                                                                                      PERMISSION_A3,
                                                                                      METER_1,
                                                                                      Instant.parse("2026-07-10T10:00:00Z"),
                                                                                      Instant.parse("2026-07-10T10:30:00Z"),
                                                                                      null);

        assertEquals(3, result.size());
        assertEquals(Instant.parse("2026-07-10T10:00:00Z"), result.get(0).getIntervalStart());
        assertEquals(Instant.parse("2026-07-10T10:10:00Z"), result.get(0).getIntervalEnd());
        assertEquals(0, new BigDecimal("2.0").compareTo(result.get(0).getMinLimitKw()));
        assertEquals(0, new BigDecimal("8.0").compareTo(result.get(0).getMaxLimitKw()));

        assertEquals(Instant.parse("2026-07-10T10:10:00Z"), result.get(1).getIntervalStart());
        assertEquals(Instant.parse("2026-07-10T10:25:00Z"), result.get(1).getIntervalEnd());
        assertEquals(0, new BigDecimal("3.0").compareTo(result.get(1).getMinLimitKw()));
        assertEquals(0, new BigDecimal("9.0").compareTo(result.get(1).getMaxLimitKw()));

        assertEquals(Instant.parse("2026-07-10T10:25:00Z"), result.get(2).getIntervalStart());
        assertEquals(Instant.parse("2026-07-10T10:30:00Z"), result.get(2).getIntervalEnd());
        assertEquals(0, new BigDecimal("1.0").compareTo(result.get(2).getMinLimitKw()));
        assertEquals(0, new BigDecimal("7.0").compareTo(result.get(2).getMaxLimitKw()));
    }

    private void savePermission(UUID permissionId, UUID userId) {
        var eddieId = UUID.fromString("ab8ef940-8f7a-4994-a626-c63bbf4f99c7");
        jdbcTemplate.update("""
                                    INSERT INTO permission (
                                        permission_id,
                                        eddie_id,
                                        status,
                                        handshake_url,
                                        access_token,
                                        user_id,
                                        transmission_enabled,
                                        data_need_id
                                    )
                                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                                    """,
                            permissionId,
                            eddieId,
                            "CREATED",
                            "https://example.org",
                            "token",
                            userId,
                            true,
                            DATA_NEED_ID);
    }

    private void saveDataNeed() {
        jdbcTemplate.update("""
                                    INSERT INTO aiida_local_data_need (
                                        data_need_id,
                                        type,
                                        name,
                                        purpose,
                                        policy_link,
                                        transmission_schedule,
                                        acknowledgement_required,
                                        asset
                                    )
                                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                                    """,
                            DATA_NEED_ID,
                            "outbound-aiida",
                            "TEST_DATA_NEED",
                            "test",
                            "https://example.org/policy",
                            "*/5 * * * * *",
                            false,
                            "CONNECTION_AGREEMENT_POINT");
    }

    private void saveLimit(UUID permissionId, String meterId, String intervalStart, String intervalEnd) {
        saveLimit(permissionId,
                  meterId,
                  intervalStart,
                  intervalEnd,
                  "3.0",
                  "8.0",
                  "2026-07-10T00:00:00Z");
    }

    private void saveLimit(
            UUID permissionId,
            String meterId,
            String intervalStart,
            String intervalEnd,
            String minLimitKw,
            String maxLimitKw,
            String createdAt
    ) {
        var limit = new ConnectionLimit(permissionId,
                                        meterId,
                                        Instant.parse(intervalStart),
                                        Instant.parse(intervalEnd),
                                        new BigDecimal(minLimitKw),
                                        new BigDecimal(maxLimitKw),
                                        "mrid-" + permissionId,
                                        1,
                                        Instant.parse(createdAt));
        connectionLimitRepository.saveAndFlush(limit);
    }
}
