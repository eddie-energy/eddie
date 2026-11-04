// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.sql.Timestamp;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@code V9_12__trim_permission_states.sql} migrates every permission state that no longer exists.
 */
@Testcontainers
class TrimPermissionStatesMigrationIntegrationTest {
    private static final String TIMESCALEDB_IMAGE = "timescale/timescaledb:latest-pg17";
    private static final String CREATE_DB_FILE = "timescaledb/create-aiida-db-and-emqx-user.sql";
    @Container
    static final PostgreSQLContainer timescale =
            new PostgreSQLContainer(DockerImageName.parse(TIMESCALEDB_IMAGE).asCompatibleSubstituteFor("postgres"))
                    .withCopyFileToContainer(MountableFile.forClasspathResource(CREATE_DB_FILE),
                                             "/docker-entrypoint-initdb.d/create-aiida-db-and-emqx-user.sql");
    private static final String MIGRATION_LOCATION = "classpath:db/aiida/migration";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OUTBOUND_DATA_NEED_ID = UUID.randomUUID();
    private static final UUID INBOUND_DATA_NEED_ID = UUID.randomUUID();
    private static final Timestamp EXISTING_REVOKE_TIME = Timestamp.valueOf("2025-01-01 10:00:00");
    private static final UUID CREATED = UUID.randomUUID();
    private static final UUID FETCHED_DETAILS = UUID.randomUUID();
    private static final UUID REJECTED = UUID.randomUUID();
    private static final UUID UNFULFILLABLE = UUID.randomUUID();
    private static final UUID FETCHED_MQTT_CREDENTIALS = UUID.randomUUID();
    private static final UUID ACCEPTED_COMPLETE = UUID.randomUUID();
    private static final UUID ACCEPTED_WITHOUT_MQTT = UUID.randomUUID();
    private static final UUID ACCEPTED_OUTBOUND_WITHOUT_DATA_SOURCE = UUID.randomUUID();
    private static final UUID ACCEPTED_INBOUND_WITHOUT_DATA_SOURCE = UUID.randomUUID();
    private static final UUID FULFILLED = UUID.randomUUID();
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var dataSource = new DriverManagerDataSource(timescale.getJdbcUrl(),
                                                     timescale.getUsername(),
                                                     timescale.getPassword());
        jdbc = new JdbcTemplate(dataSource);

        Flyway.configure().locations(MIGRATION_LOCATION).dataSource(dataSource).target("9.11").load().migrate();

        insertDataNeed(OUTBOUND_DATA_NEED_ID, "outbound-aiida");
        insertDataNeed(INBOUND_DATA_NEED_ID, "inbound-aiida");
        var dataSourceId = insertDataSource();

        insertPermission(CREATED, "CREATED", OUTBOUND_DATA_NEED_ID, null, null);
        insertPermission(FETCHED_DETAILS, "FETCHED_DETAILS", OUTBOUND_DATA_NEED_ID, null, null);
        insertPermission(REJECTED, "REJECTED", OUTBOUND_DATA_NEED_ID, null, EXISTING_REVOKE_TIME);
        insertPermission(UNFULFILLABLE, "UNFULFILLABLE", OUTBOUND_DATA_NEED_ID, null, EXISTING_REVOKE_TIME);
        insertPermission(FETCHED_MQTT_CREDENTIALS, "FETCHED_MQTT_CREDENTIALS", OUTBOUND_DATA_NEED_ID, dataSourceId, null);
        insertMqttConfig(FETCHED_MQTT_CREDENTIALS);
        insertPermission(ACCEPTED_COMPLETE, "ACCEPTED", OUTBOUND_DATA_NEED_ID, dataSourceId, null);
        insertMqttConfig(ACCEPTED_COMPLETE);
        insertPermission(ACCEPTED_WITHOUT_MQTT, "ACCEPTED", OUTBOUND_DATA_NEED_ID, dataSourceId, null);
        insertPermission(ACCEPTED_OUTBOUND_WITHOUT_DATA_SOURCE, "ACCEPTED", OUTBOUND_DATA_NEED_ID, null, null);
        insertMqttConfig(ACCEPTED_OUTBOUND_WITHOUT_DATA_SOURCE);
        insertPermission(ACCEPTED_INBOUND_WITHOUT_DATA_SOURCE, "ACCEPTED", INBOUND_DATA_NEED_ID, null, null);
        insertMqttConfig(ACCEPTED_INBOUND_WITHOUT_DATA_SOURCE);
        insertPermission(FULFILLED, "FULFILLED", OUTBOUND_DATA_NEED_ID, dataSourceId, null);

        Flyway.configure().locations(MIGRATION_LOCATION).dataSource(dataSource).load().migrate();
    }

    @Test
    void neverAcceptedPermissions_areRevoked_andKeepExistingRevokeTime() {
        assertRevoked(CREATED);
        assertRevoked(FETCHED_DETAILS);
        assertEquals(EXISTING_REVOKE_TIME, revokeTime(REJECTED));
        assertEquals("REVOKED", status(REJECTED));
        assertEquals(EXISTING_REVOKE_TIME, revokeTime(UNFULFILLABLE));
        assertEquals("REVOKED", status(UNFULFILLABLE));
    }

    @Test
    void fetchedMqttCredentials_becomesAccepted() {
        assertEquals("ACCEPTED", status(FETCHED_MQTT_CREDENTIALS));
    }

    @Test
    void completeAcceptedPermission_staysAccepted() {
        assertEquals("ACCEPTED", status(ACCEPTED_COMPLETE));
        assertEquals("FULFILLED", status(FULFILLED));
    }

    @Test
    void incompleteAcceptedPermissions_areRevoked() {
        assertRevoked(ACCEPTED_WITHOUT_MQTT);
        assertRevoked(ACCEPTED_OUTBOUND_WITHOUT_DATA_SOURCE);
    }

    @Test
    void inboundAcceptedPermissionWithoutDataSource_staysAccepted() {
        // inbound data sources are created on demand, so a missing data source is not an incomplete permission
        assertEquals("ACCEPTED", status(ACCEPTED_INBOUND_WITHOUT_DATA_SOURCE));
    }

    @Test
    void removedStatus_isRejectedByCheckConstraint() {
        assertThrows(Exception.class,
                     () -> insertPermission(UUID.randomUUID(), "CREATED", OUTBOUND_DATA_NEED_ID, null, null));
        assertThat(jdbc.queryForList("SELECT DISTINCT status FROM permission", String.class))
                .allMatch(status -> status.equals("ACCEPTED") || status.equals("REVOKED")
                                    || status.equals("TERMINATED") || status.equals("FULFILLED"));
    }

    private static void assertRevoked(UUID permissionId) {
        assertEquals("REVOKED", status(permissionId));
        assertNotNull(revokeTime(permissionId));
    }

    private static String status(UUID permissionId) {
        return jdbc.queryForObject("SELECT status FROM permission WHERE permission_id = ?", String.class, permissionId);
    }

    private static Timestamp revokeTime(UUID permissionId) {
        return jdbc.queryForObject("SELECT revoke_time FROM permission WHERE permission_id = ?",
                                   Timestamp.class,
                                   permissionId);
    }

    private static void insertDataNeed(UUID dataNeedId, String type) {
        jdbc.update("""
                    INSERT INTO aiida_local_data_need (data_need_id, asset, name, policy_link, purpose, transmission_schedule, type)
                    VALUES (?, 'CONNECTION_AGREEMENT_POINT', 'name', 'https://example.com', 'purpose', '*/5 * * * * *', ?)
                    """, dataNeedId, type);
    }

    private static UUID insertDataSource() {
        var dataSourceId = UUID.randomUUID();
        jdbc.update("""
                    INSERT INTO data_source (id, user_id, asset, country_code, enabled, icon, name, type)
                    VALUES (?, ?, 'CONNECTION_AGREEMENT_POINT', 'AT', true, 'METER', 'simulation', 'SIMULATION')
                    """, dataSourceId, USER_ID);
        return dataSourceId;
    }

    private static void insertPermission(
            UUID permissionId,
            String status,
            UUID dataNeedId,
            UUID dataSourceId,
            Timestamp revokeTime
    ) {
        jdbc.update("""
                    INSERT INTO permission (permission_id, eddie_id, status, handshake_url, access_token, user_id,
                                            transmission_enabled, data_need_id, data_source_id, revoke_time)
                    VALUES (?, ?, ?, 'https://example.com', 'token', ?, true, ?, ?, ?)
                    """, permissionId, UUID.randomUUID(), status, USER_ID, dataNeedId, dataSourceId, revokeTime);
    }

    private static void insertMqttConfig(UUID permissionId) {
        jdbc.update("""
                    INSERT INTO mqtt_streaming_config (permission_id, password, server_uri, data_topic, status_topic, command_topic)
                    VALUES (?, 'password', 'tcp://localhost:1883', 'data', 'status', 'command')
                    """, permissionId);
    }
}
