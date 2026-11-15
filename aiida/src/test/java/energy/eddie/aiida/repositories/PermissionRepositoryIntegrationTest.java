// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeedFactory;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that accepted permissions, which have an assigned ID, are persisted together with a new data need, their data
 * source and their MQTT streaming config, and that loaded permissions are updated instead of inserted again.
 *
 * @see energy.eddie.aiida.services.PermissionService#acceptPermission
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/aiida/migration")
@Testcontainers
class PermissionRepositoryIntegrationTest {
    private static final String TIMESCALEDB_IMAGE = "timescale/timescaledb:latest-pg17";
    private static final String TIMESCALEDB_CREATE_AIIDA_DB_AND_EMQX_USER_FILE =
            "timescaledb/create-aiida-db-and-emqx-user.sql";
    private static final String TIMESCALEDB_CONTAINER_PATH =
            "/docker-entrypoint-initdb.d/create-aiida-db-and-emqx-user.sql";
    @Container
    @ServiceConnection
    static final PostgreSQLContainer timescale = new PostgreSQLContainer(
            DockerImageName.parse(TIMESCALEDB_IMAGE).asCompatibleSubstituteFor("postgres")
    ).withCopyFileToContainer(
            MountableFile.forClasspathResource(TIMESCALEDB_CREATE_AIIDA_DB_AND_EMQX_USER_FILE),
            TIMESCALEDB_CONTAINER_PATH
    );
    private static final UUID DATA_NEED_ID = UUID.fromString("f7698978-b9fe-40c8-aebe-c997f7f58f2f");
    private static final String INBOUND_DATA_NEED_JSON = """
            {
              "type": "inbound-aiida",
              "id": "f7698978-b9fe-40c8-aebe-c997f7f58f2f",
              "name": "FUTURE_MIN_MAX_ENVELOPE_INBOUND",
              "description": "Inbound min-max envelopes",
              "purpose": "purpose",
              "policyLink": "https://example.com/toc",
              "enabled": true,
              "duration": {"type": "relativeDuration", "start": "P0D", "end": "P10D"},
              "transmissionSchedule": "*/5 * * * * *",
              "acknowledgementRequired": true,
              "allowedPermissionCommands": ["UPDATE_LIMIT_DEFAULTS"],
              "asset": "CONNECTION-AGREEMENT-POINT",
              "schemas": ["OPAQUE", "MIN-MAX-ENVELOPE-CIM-V1-12"]
            }
            """;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void save_newPermissionWithNewDataNeedAndMqttConfig_persistsAll() {
        // Given
        // The data_need_id column defaults to the first stored data need, which must not be used for new permissions
        insertOtherDataNeed();
        var dataSourceId = insertDataSource();
        var permissionId = UUID.randomUUID();
        var permission = acceptedPermission(permissionId);
        permission.setDataSource(entityManager.find(DataSource.class, dataSourceId));

        // When
        saveAccepted(permission);
        entityManager.flush();
        entityManager.clear();

        // Then
        var stored = permissionRepository.findById(permissionId).orElseThrow();
        assertEquals(PermissionStatus.ACCEPTED, stored.status());
        assertInstanceOf(InboundAiidaLocalDataNeed.class, stored.dataNeed());
        assertEquals(DATA_NEED_ID, stored.dataNeed().dataNeedId());
        assertNotNull(stored.dataSource());
        assertEquals(dataSourceId, stored.dataSource().id());
        assertNotNull(stored.mqttStreamingConfig());
        assertEquals(permissionId, stored.mqttStreamingConfig().permissionId());
        assertFalse(stored.isNew());
    }

    @Test
    void save_loadedPermission_updatesInsteadOfInserting() {
        // Given
        var permissionId = UUID.randomUUID();
        saveAccepted(acceptedPermission(permissionId));
        entityManager.flush();
        entityManager.clear();
        var loaded = permissionRepository.findById(permissionId).orElseThrow();

        // When
        loaded.setStatus(PermissionStatus.REVOKED);
        permissionRepository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(permissionRepository.findAll()).hasSize(1);
        assertEquals(PermissionStatus.REVOKED, permissionRepository.findById(permissionId).orElseThrow().status());
    }

    private void insertOtherDataNeed() {
        jdbcTemplate.update("""
                            INSERT INTO aiida_local_data_need (data_need_id, asset, name, policy_link, purpose, transmission_schedule, type)
                            VALUES (?, 'CONNECTION_AGREEMENT_POINT', 'other', 'https://example.com', 'purpose', '*/5 * * * * *', 'outbound-aiida')
                            """, UUID.randomUUID());
    }

    private UUID insertDataSource() {
        var dataSourceId = UUID.randomUUID();
        jdbcTemplate.update("""
                            INSERT INTO data_source (id, user_id, asset, country_code, enabled, icon, name, type)
                            VALUES (?, ?, 'CONNECTION_AGREEMENT_POINT', 'AT', true, 'METER', 'simulation', 'SIMULATION')
                            """, dataSourceId, UUID.randomUUID());
        return dataSourceId;
    }

    /**
     * Mirrors the accept flow: the MQTT streaming config is attached after the permission has been persisted.
     */
    private void saveAccepted(Permission permission) {
        var saved = permissionRepository.save(permission);
        saved.setMqttStreamingConfig(mqttStreamingConfig(saved.id()));
    }

    private static Permission acceptedPermission(UUID permissionId) {
        var dataNeed = ObjectMapperCreatorUtil.mapper().readValue(INBOUND_DATA_NEED_JSON, AiidaDataNeed.class);
        var permission = new Permission(UUID.randomUUID(),
                                        permissionId,
                                        "http://localhost:8080/region-connectors/aiida/permission-request/" + permissionId,
                                        "token",
                                        UUID.randomUUID());
        var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        permission.setConnectionId("connection-id");
        permission.setStartTime(now);
        permission.setExpirationTime(now.plus(10, ChronoUnit.DAYS));
        permission.initializeFromDataNeed(AiidaLocalDataNeedFactory.create(dataNeed));
        permission.setGrantTime(now);
        return permission;
    }

    private static MqttStreamingConfig mqttStreamingConfig(UUID permissionId) {
        return new MqttStreamingConfig(new MqttDto("tcp://localhost:1883",
                                                   permissionId.toString(),
                                                   "password",
                                                   "data",
                                                   "status",
                                                   "command",
                                                   "acknowledgement"));
    }
}
