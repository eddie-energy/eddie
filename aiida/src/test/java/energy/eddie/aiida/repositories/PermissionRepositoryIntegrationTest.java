// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.*;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
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
import java.util.UUID;

import static energy.eddie.aiida.models.permission.dataneed.LocalDataNeedTestData.inboundDataNeed;
import static energy.eddie.aiida.models.permission.dataneed.LocalDataNeedTestData.outboundDataNeed;
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
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private InboundPermissionRepository inboundPermissionRepository;
    @Autowired
    private OutboundPermissionRepository outboundPermissionRepository;
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
        var permission = outboundPermission(permissionId, UUID.randomUUID());
        permission.setDataSource(entityManager.find(DataSource.class, dataSourceId));

        // When
        saveAccepted(permission);
        entityManager.flush();
        entityManager.clear();

        // Then
        var stored = permissionRepository.findById(permissionId).orElseThrow();
        assertEquals(PermissionStatus.ACCEPTED, stored.status());
        assertInstanceOf(OutboundAiidaLocalDataNeed.class, stored.dataNeed());
        assertEquals(outboundDataNeed().dataNeedId(), stored.dataNeed().dataNeedId());
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

    @Test
    void save_inboundAndOutboundPermission_persistsDiscriminatorAndLoadsSubtype() {
        // Given
        var inboundPermissionId = UUID.randomUUID();
        var outboundPermissionId = UUID.randomUUID();
        saveAccepted(acceptedPermission(inboundPermissionId));
        saveAccepted(outboundPermission(outboundPermissionId, UUID.randomUUID()));
        entityManager.flush();
        entityManager.clear();

        // When
        var inbound = permissionRepository.findById(inboundPermissionId).orElseThrow();
        var outbound = permissionRepository.findById(outboundPermissionId).orElseThrow();

        // Then
        assertInstanceOf(InboundPermission.class, inbound);
        assertInstanceOf(OutboundPermission.class, outbound);
        assertEquals(InboundPermission.DISCRIMINATOR_VALUE, typeColumn(inboundPermissionId));
        assertEquals(OutboundPermission.DISCRIMINATOR_VALUE, typeColumn(outboundPermissionId));
        assertNull(jdbcTemplate.queryForObject("SELECT transmission_enabled FROM permission WHERE permission_id = ?",
                                               Boolean.class,
                                               inboundPermissionId));
        assertTrue(((OutboundPermission) outbound).transmissionEnabled());
    }

    @Test
    void typedRepositories_returnOnlyPermissionsOfTheirSubtype() {
        // Given
        var userId = UUID.randomUUID();
        var dataSource = entityManager.find(DataSource.class, insertDataSource());
        var inboundPermission = new InboundPermission(UUID.randomUUID(),
                                                      UUID.randomUUID(),
                                                      "https://example.org",
                                                      "token",
                                                      userId,
                                                      inboundDataNeed(),
                                                      "connection-id",
                                                      Instant.parse("2026-01-01T00:00:00Z"),
                                                      Instant.parse("2026-12-31T00:00:00Z"),
                                                      Instant.parse("2026-01-01T00:00:00Z"));
        inboundPermission.setDataSource(dataSource);
        var outboundPermission = outboundPermission(UUID.randomUUID(), userId);
        outboundPermission.setDataSource(dataSource);
        saveAccepted(inboundPermission);
        saveAccepted(outboundPermission);
        entityManager.flush();
        entityManager.clear();

        // When
        var inbound = inboundPermissionRepository.findByUserIdAndDataSourceIsNotNullAndStatusOrderByGrantTimeDesc(
                userId,
                PermissionStatus.ACCEPTED);
        var outbound = outboundPermissionRepository.findByDataSourceIdAndStatus(dataSource.id(),
                                                                                PermissionStatus.ACCEPTED);
        var inboundById = inboundPermissionRepository.findById(outboundPermission.id());
        var outboundById = outboundPermissionRepository.findById(inboundPermission.id());

        // Then
        assertThat(inbound).extracting(Permission::id).containsExactly(inboundPermission.id());
        assertThat(outbound).extracting(Permission::id).containsExactly(outboundPermission.id());
        assertThat(inboundById).isEmpty();
        assertThat(outboundById).isEmpty();
    }

    private String typeColumn(UUID permissionId) {
        return jdbcTemplate.queryForObject("SELECT type FROM permission WHERE permission_id = ?",
                                           String.class,
                                           permissionId);
    }

    private static OutboundPermission outboundPermission(UUID permissionId, UUID userId) {
        return new OutboundPermission(UUID.randomUUID(),
                                      permissionId,
                                      handshakeUrl(permissionId),
                                      "token",
                                      userId,
                                      outboundDataNeed(),
                                      "connection-id",
                                      Instant.parse("2026-01-01T00:00:00Z"),
                                      Instant.parse("2026-12-31T00:00:00Z"),
                                      Instant.parse("2026-01-01T00:00:00Z"));
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

    private static InboundPermission acceptedPermission(UUID permissionId) {
        return new InboundPermission(UUID.randomUUID(),
                                     permissionId,
                                     handshakeUrl(permissionId),
                                     "token",
                                     UUID.randomUUID(),
                                     inboundDataNeed(),
                                     "connection-id",
                                     Instant.parse("2026-01-01T00:00:00Z"),
                                     Instant.parse("2026-12-31T00:00:00Z"),
                                     Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static String handshakeUrl(UUID permissionId) {
        return "http://localhost:8080/region-connectors/aiida/permission-request/" + permissionId;
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
