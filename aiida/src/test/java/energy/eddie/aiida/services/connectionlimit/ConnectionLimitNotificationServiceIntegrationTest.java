// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.config.OAuth2SecurityConfiguration;
import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.datasource.DataSourceIcon;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.interval.simulation.SimulationDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.user.UserSettings;
import energy.eddie.aiida.repositories.*;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.cors.CorsConfigurationSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        // The classpath also contains the data-needs migrations, so pin the aiida schema explicitly.
        "spring.flyway.locations=classpath:db/aiida/migration",
        // Effectively disable the scheduled sweep, the test triggers it explicitly.
        "aiida.notification.interval-ms=3600000"
})
@MockitoBean(types = {ClientRegistrationRepository.class, OAuth2SecurityConfiguration.class,
        CorsConfigurationSource.class})
class ConnectionLimitNotificationServiceIntegrationTest {
    private static final String TIMESCALEDB_IMAGE = "timescale/timescaledb:latest-pg17";
    private static final String TIMESCALEDB_INIT_FILE = "timescaledb/create-aiida-db-and-emqx-user.sql";
    private static final String TIMESCALEDB_INIT_PATH = "/docker-entrypoint-initdb.d/create-aiida-db-and-emqx-user.sql";

    private static final UUID USER_ID = UUID.fromString("b69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    private static final UUID EDDIE_ID = UUID.fromString("9609a9b3-0718-4082-935d-6a98c0f8c5a1");
    private static final UUID DATA_NEED_ID = UUID.fromString("6609a9b3-0718-4082-935d-6a98c0f8c5a1");
    private static final UUID PERMISSION_ID = UUID.fromString("8609a9b3-0718-4082-935d-6a98c0f8c5a1");
    private static final String METER_ID = "test-meter";
    private static final String EMAIL = "user@example.com";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer timescale =
            new PostgreSQLContainer(DockerImageName.parse(TIMESCALEDB_IMAGE)
                                                   .asCompatibleSubstituteFor("postgres"))
                    .withCopyFileToContainer(MountableFile.forClasspathResource(TIMESCALEDB_INIT_FILE),
                                             TIMESCALEDB_INIT_PATH);

    @Autowired
    private ConnectionLimitNotificationService service;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DataSourceRepository dataSourceRepository;
    @Autowired
    private ConnectionLimitRepository connectionLimitRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private UserSettingsRepository userSettingsRepository;
    @Autowired
    private AiidaRecordRepository aiidaRecordRepository;
    @MockitoBean
    private JavaMailSender mailSender;

    private UUID dataSourceId;

    @BeforeEach
    void setUp() {
        seedPermission();
        seedDataSource();
        seedLimits();
        seedMonitoring();
        seedUserSettings();
    }

    @Test
    void givenViolationThenRecovery_notifiesUser() {
        // First sweep only establishes the watermark, so pre-existing records are not replayed.
        service.checkViolations();

        insertRecord("9.0");
        service.checkViolations();

        var violation = sentMessage();
        assertEquals(EMAIL, violation.getTo()[0]);
        assertEquals("Connection limits exceeded", violation.getSubject());

        insertRecord("5.0");
        service.checkViolations();

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());
        assertEquals("Connection limits restored", captor.getAllValues().get(1).getSubject());
    }

    private void seedPermission() {
        jdbcTemplate.update("""
                                    INSERT INTO aiida_local_data_need (data_need_id, asset, name, policy_link, purpose,
                                                                       transmission_schedule, acknowledgement_required, type)
                                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                                    """,
                            DATA_NEED_ID,
                            "CONNECTION_AGREEMENT_POINT",
                            "TEST_INBOUND",
                            "https://example.org",
                            "purpose",
                            "*/5 * * * * *",
                            false,
                            "inbound-aiida");

        jdbcTemplate.update("""
                                    INSERT INTO permission (permission_id, eddie_id, data_need_id, access_token,
                                                            handshake_url, status, user_id, meter_id, transmission_enabled)
                                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                                    """,
                            PERMISSION_ID,
                            EDDIE_ID,
                            DATA_NEED_ID,
                            "accessToken",
                            "https://example.org/handshake",
                            "STREAMING_DATA",
                            USER_ID,
                            METER_ID,
                            true);
    }

    private void seedDataSource() {
        var dto = mock(SimulationDataSourceDto.class);
        when(dto.name()).thenReturn("Test Simulation");
        when(dto.enabled()).thenReturn(true);
        when(dto.type()).thenReturn(DataSourceType.SIMULATION);
        when(dto.countryCode()).thenReturn("AT");
        when(dto.icon()).thenReturn(DataSourceIcon.ELECTRICITY);
        when(dto.asset()).thenReturn(AiidaAsset.CONNECTION_AGREEMENT_POINT);
        when(dto.meterId()).thenReturn(METER_ID);
        when(dto.pollingInterval()).thenReturn(5);

        dataSourceId = dataSourceRepository.save(new SimulationDataSource(dto, USER_ID)).id();
    }

    private void seedLimits() {
        var now = Instant.now();
        connectionLimitRepository.save(new ConnectionLimit(PERMISSION_ID,
                                                           METER_ID,
                                                           now.minusSeconds(3600),
                                                           now.plusSeconds(3600),
                                                           BigDecimal.valueOf(3),
                                                           BigDecimal.valueOf(8),
                                                           "test-mrid",
                                                           1,
                                                           now));
    }

    private void seedMonitoring() {
        var permission = permissionRepository.findById(PERMISSION_ID).orElseThrow();
        permission.updateMonitoringDataSource(dataSourceId);
        permissionRepository.save(permission);
    }

    private void seedUserSettings() {
        userSettingsRepository.save(new UserSettings(USER_ID, EMAIL));
    }

    private void insertRecord(String importedPower) {
        var dataSource = dataSourceRepository.findById(dataSourceId).orElseThrow();
        var values = new ArrayList<AiidaRecordValue>();
        values.add(new AiidaRecordValue(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER.toString(),
                                        ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                        importedPower,
                                        UnitOfMeasurement.KILO_WATT,
                                        importedPower,
                                        UnitOfMeasurement.KILO_WATT));

        var record = new AiidaRecord(Instant.now(), dataSource, values);
        values.forEach(value -> value.setAiidaRecord(record));
        aiidaRecordRepository.saveAndFlush(record);
    }

    private SimpleMailMessage sentMessage() {
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
