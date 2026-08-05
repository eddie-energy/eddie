// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.provisioning.ProvisioningTypePatchDto;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.repositories.AiidaLocalDataNeedRepository;
import energy.eddie.aiida.repositories.InboundDataSourceRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.utils.MqttFactory;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import jakarta.persistence.EntityManager;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import reactor.core.publisher.Flux;

import java.util.Set;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/aiida/migration")
@Import({ProvisioningService.class, ProvisioningServiceIntegrationTest.TestConfig.class})
@Testcontainers
class ProvisioningServiceIntegrationTest {
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
    private static final String CLIENT_HOST = "tcp://broker.example.test:1883";
    private static final String CLIENT_USERNAME = "provisioning-user";
    private static final String CLIENT_PASSWORD = "provisioning-password";
    private static final String CLIENT_TOPIC = "aiida/inbound/test";
    private static final String SERVER_INTERNAL_HOST = "tcp://aiida-internal.test:1883";
    private static final String SERVER_EXTERNAL_HOST = "tcp://aiida-external.test:1883";
    private static final String ENCODED_SERVER_PASSWORD = "encoded-server-password";
    private static final String AIIDA_USERNAME = "aiida";
    private static final String AIIDA_PASSWORD = "aiida-password";
    @Autowired
    private ProvisioningService provisioningService;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private AiidaLocalDataNeedRepository aiidaLocalDataNeedRepository;
    @Autowired
    private InboundDataSourceRepository inboundDataSourceRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MqttConfiguration mqttConfiguration;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    void mqttClient_persistsProvisioningConnectionAndAccessControlEntry() throws Exception {
        var permissionId = createInboundPermission();
        var mqttClient = mock(MqttAsyncClient.class);

        try (MockedStatic<MqttFactory> ignored = mqttFactoryReturning(mqttClient)) {
            var result = provisioningService.changeProvisioningType(
                    permissionId,
                    provisioningPatch(permissionId, InboundProvisioningType.MQTT_CLIENT)
            );
            assertThat(result.host()).isEqualTo(CLIENT_HOST);
            assertThat(result.username()).isEqualTo(CLIENT_USERNAME);
            assertThat(result.password()).isEqualTo(CLIENT_PASSWORD);
            assertThat(result.topic()).isEqualTo(CLIENT_TOPIC);

            entityManager.flush();
            entityManager.clear();

            var dataSource = inboundDataSourceRepository.findById(dataSourceId(permissionId)).orElseThrow();
            assertThat(dataSource.inboundProvisioningType()).isEqualTo(InboundProvisioningType.MQTT_CLIENT);
            assertThat(dataSource.provisioningConnection().internalHost()).isEqualTo(CLIENT_HOST);
            assertThat(dataSource.provisioningConnection().username()).isEqualTo(CLIENT_USERNAME);
            assertThat(dataSource.provisioningConnection().password()).isEqualTo(CLIENT_PASSWORD);
            assertThat(dataSource.provisioningTopicOrThrow()).isEqualTo(CLIENT_TOPIC);
            verify(mqttClient).connect(any());
        }
    }

    @Test
    void loadingPermissionWithInboundDataSource_eagerlyLoadsStreamingConfig() {
        var permissionId = createInboundPermission();

        entityManager.flush();
        entityManager.clear();

        var permission = permissionRepository.findById(permissionId).orElseThrow();

        assertThat(permission.dataSource())
                .isInstanceOfSatisfying(
                        InboundDataSource.class,
                        dataSource -> assertThat(dataSource.acknowledgementTopic()).isEqualTo("aiida/ack")
                );
    }

    @Test
    void loadingInboundDataSource_eagerlyLoadsStreamingConfigForDetachedAdapterCreation() {
        var permissionId = createInboundPermission();
        var dataSourceId = dataSourceId(permissionId);

        entityManager.flush();
        entityManager.clear();

        var dataSource = inboundDataSourceRepository.findById(dataSourceId).orElseThrow();
        entityManager.clear();

        assertThat(dataSource.acknowledgementTopic()).isEqualTo("aiida/ack");
    }

    @Test
    void loadingMqttProvisioningDataSourcesForStartup_eagerlyLoadsMqttConfig() throws Exception {
        var permissionId = createInboundPermission();
        var mqttClient = mock(MqttAsyncClient.class);

        try (MockedStatic<MqttFactory> ignored = mqttFactoryReturning(mqttClient)) {
            provisioningService.changeProvisioningType(
                    permissionId,
                    provisioningPatch(permissionId, InboundProvisioningType.MQTT_CLIENT)
            );
            entityManager.flush();
            entityManager.clear();

            var dataSource = inboundDataSourceRepository
                    .findByProvisioningTypeIn(Set.of(InboundProvisioningType.MQTT_CLIENT))
                    .getFirst();
            entityManager.clear();

            assertThat(dataSource.provisioningConnection().internalHost()).isEqualTo(CLIENT_HOST);
            assertThat(dataSource.provisioningConnection().username()).isEqualTo(CLIENT_USERNAME);
            assertThat(dataSource.provisioningTopicOrThrow()).isEqualTo(CLIENT_TOPIC);
        }
    }

    @Test
    void mqttServer_persistsGeneratedCredentialsAndBrokerConfiguration() throws Exception {
        var permissionId = createInboundPermission();
        var dataSourceId = dataSourceId(permissionId);
        var mqttClient = mock(MqttAsyncClient.class);
        var plaintextPasswordCaptor = ArgumentCaptor.forClass(String.class);

        when(mqttConfiguration.internalHost()).thenReturn(SERVER_INTERNAL_HOST);
        when(mqttConfiguration.externalHost()).thenReturn(SERVER_EXTERNAL_HOST);
        when(mqttConfiguration.username()).thenReturn(AIIDA_USERNAME);
        when(mqttConfiguration.password()).thenReturn(AIIDA_PASSWORD);
        when(passwordEncoder.encode(plaintextPasswordCaptor.capture())).thenReturn(ENCODED_SERVER_PASSWORD);

        try (MockedStatic<MqttFactory> ignored = mqttFactoryReturning(mqttClient)) {
            var result = provisioningService.changeProvisioningType(
                    permissionId,
                    provisioningPatch(permissionId, InboundProvisioningType.MQTT_SERVER)
            );
            var responseUsername = result.username();
            var responsePassword = result.password();
            assertThat(result.host()).isEqualTo(SERVER_EXTERNAL_HOST);
            assertThat(result.topic()).isEqualTo("aiida/" + responseUsername + "/inboundData");
            assertThat(responseUsername).isNotBlank();
            assertThatCode(() -> UUID.fromString(responseUsername)).doesNotThrowAnyException();
            assertThat(responsePassword).isEqualTo(plaintextPasswordCaptor.getValue());
            assertThat(plaintextPasswordCaptor.getValue()).hasSize(10);
            assertThat(plaintextPasswordCaptor.getValue()).isNotEqualTo(ENCODED_SERVER_PASSWORD);

            entityManager.flush();
            entityManager.clear();

            var dataSource = inboundDataSourceRepository.findById(dataSourceId).orElseThrow();
            assertThat(dataSource.inboundProvisioningType()).isEqualTo(InboundProvisioningType.MQTT_SERVER);
            assertThat(dataSource.provisioningConnection().internalHost()).isEqualTo(SERVER_INTERNAL_HOST);
            assertThat(dataSource.provisioningConnection().username()).isEqualTo(responseUsername);
            assertThat(dataSource.provisioningConnection().password()).isEqualTo(ENCODED_SERVER_PASSWORD);
            assertThat(dataSource.provisioningTopicOrThrow()).isEqualTo("aiida/" + responseUsername + "/inboundData");
            assertThat(provisioningExternalHost(dataSourceId)).isEqualTo(SERVER_EXTERNAL_HOST);
            assertThat(provisioningAclUsername(dataSourceId)).isEqualTo(responseUsername);

            var optionsCaptor = ArgumentCaptor.forClass(MqttConnectionOptions.class);
            verify(mqttClient).connect(optionsCaptor.capture());
            assertThat(optionsCaptor.getValue().getUserName()).isEqualTo(AIIDA_USERNAME);
            assertThat(new String(optionsCaptor.getValue().getPassword(), UTF_8))
                    .isEqualTo(AIIDA_PASSWORD);
        }
    }

    @Test
    void mqttClientToRest_removesProvisioningRowsAndClosesPublisher() throws Exception {
        var permissionId = createInboundPermission();
        var dataSourceId = dataSourceId(permissionId);
        var mqttClient = mock(MqttAsyncClient.class);
        when(mqttClient.isConnected()).thenReturn(true);

        try (MockedStatic<MqttFactory> ignored = mqttFactoryReturning(mqttClient)) {
            provisioningService.changeProvisioningType(
                    permissionId,
                    provisioningPatch(permissionId, InboundProvisioningType.MQTT_CLIENT)
            );
            entityManager.flush();

            var provisioningConfigId = jdbcTemplate.queryForObject(
                    "SELECT mqtt_provisioning_config_id FROM data_source_mqtt_inbound WHERE id = ?",
                    Long.class,
                    dataSourceId
            );
            var connectionId = jdbcTemplate.queryForObject(
                    "SELECT mqtt_connection_id FROM data_source_mqtt_inbound_provisioning WHERE id = ?",
                    Long.class,
                    provisioningConfigId
            );
            var accessControlEntryId = jdbcTemplate.queryForObject(
                    "SELECT mqtt_acl_id FROM data_source_mqtt_inbound_provisioning WHERE id = ?",
                    Long.class,
                    provisioningConfigId
            );
            var mqttUserId = jdbcTemplate.queryForObject(
                    "SELECT mqtt_user_id FROM mqtt_connection WHERE id = ?",
                    Long.class,
                    connectionId
            );

            var result = provisioningService.changeProvisioningType(
                    permissionId,
                    provisioningPatch(permissionId, InboundProvisioningType.REST_API_TOKEN)
            );
            assertThat(result.host()).isEmpty();
            assertThat(result.username()).isEmpty();
            assertThat(result.password()).isEmpty();
            assertThat(result.topic()).isEmpty();

            entityManager.flush();
            entityManager.clear();

            var dataSource = inboundDataSourceRepository.findById(dataSourceId).orElseThrow();
            assertThat(dataSource.inboundProvisioningType()).isEqualTo(InboundProvisioningType.REST_API_TOKEN);
            assertThat(nullableForeignKey("mqtt_provisioning_config_id", dataSourceId)).isNull();
            assertThat(rowCount("data_source_mqtt_inbound_provisioning", provisioningConfigId)).isZero();
            assertThat(rowCount("mqtt_connection", connectionId)).isZero();
            assertThat(rowCount("data_source_mqtt_acl", accessControlEntryId)).isZero();
            assertThat(rowCount("data_source_mqtt_user", mqttUserId)).isZero();
            verify(mqttClient).disconnect(anyLong());
            verify(mqttClient).close();
        }
    }

    private static ProvisioningTypePatchDto provisioningPatch(
            UUID permissionId,
            InboundProvisioningType provisioningType
    ) {
        return new ProvisioningTypePatchDto(
                permissionId,
                provisioningType,
                CLIENT_HOST,
                CLIENT_USERNAME,
                CLIENT_PASSWORD,
                CLIENT_TOPIC
        );
    }

    private UUID createInboundPermission() {
        var permission = createPermission();

        var dataSource = new InboundDataSource.Builder(permission).build();
        dataSource = inboundDataSourceRepository.saveAndFlush(dataSource);
        permission.setDataSource(dataSource);
        permissionRepository.saveAndFlush(permission);

        return permission.id();
    }

    private Permission createPermission() {
        var permissionId = UUID.randomUUID();
        var permission = new Permission(
                UUID.randomUUID(),
                permissionId,
                "https://example.test/handshake",
                "access-token",
                UUID.randomUUID()
        );
        permissionRepository.saveAndFlush(permission);

        permission.setDataNeed(aiidaLocalDataNeedRepository.saveAndFlush(inboundDataNeed()));
        permissionRepository.saveAndFlush(permission);

        permission.setMqttStreamingConfig(new MqttStreamingConfig(new MqttDto(
                "tcp://aiida-broker.test:1883",
                permissionId.toString(),
                "streaming-password",
                "aiida/data",
                "aiida/status",
                "aiida/command",
                "aiida/ack"
        )));
        return permissionRepository.saveAndFlush(permission);
    }

    private static InboundAiidaLocalDataNeed inboundDataNeed() {
        var dataNeed = mock(AiidaDataNeed.class);
        when(dataNeed.dataNeedId()).thenReturn(UUID.randomUUID());
        when(dataNeed.type()).thenReturn("inbound-aiida");
        when(dataNeed.name()).thenReturn("Integration test inbound data need");
        when(dataNeed.purpose()).thenReturn("Provisioning integration test");
        when(dataNeed.policyLink()).thenReturn("https://example.test/policy");
        when(dataNeed.transmissionSchedule()).thenReturn(
                CronExpression.parse("0 * * * * *")
        );
        when(dataNeed.schemas()).thenReturn(Set.of(AiidaSchema.OPAQUE));
        when(dataNeed.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(dataNeed.dataTags()).thenReturn(Set.of());
        when(dataNeed.allowedPermissionCommands()).thenReturn(Set.of());
        when(dataNeed.contexts()).thenReturn(Set.of());
        return new InboundAiidaLocalDataNeed(dataNeed);
    }

    private UUID dataSourceId(UUID permissionId) {
        entityManager.flush();
        entityManager.clear();
        var dataSource = permissionRepository.findById(permissionId).orElseThrow().dataSource();
        assertThat(dataSource).isInstanceOf(InboundDataSource.class);
        return dataSource.id();
    }

    private Long nullableForeignKey(String column, UUID dataSourceId) {
        return jdbcTemplate.query(
                "select " + column + " from data_source_mqtt_inbound where id = ?",
                resultSet -> resultSet.next() ? resultSet.getObject(1, Long.class) : null,
                dataSourceId
        );
    }

    private int rowCount(String table, Long id) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where id = ?",
                Integer.class,
                id
        );
    }

    private String provisioningExternalHost(UUID dataSourceId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT connection.external_host
                        FROM data_source_mqtt_inbound inbound
                        JOIN data_source_mqtt_inbound_provisioning config
                            ON config.id = inbound.mqtt_provisioning_config_id
                        JOIN mqtt_connection connection ON connection.id = config.mqtt_connection_id
                        WHERE inbound.id = ?
                        """,
                String.class,
                dataSourceId
        );
    }

    private String provisioningAclUsername(UUID dataSourceId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT acl.username
                        FROM data_source_mqtt_inbound inbound
                        JOIN data_source_mqtt_inbound_provisioning config
                            ON config.id = inbound.mqtt_provisioning_config_id
                        JOIN data_source_mqtt_acl acl ON acl.id = config.mqtt_acl_id
                        WHERE inbound.id = ?
                        """,
                String.class,
                dataSourceId
        );
    }

    private static MockedStatic<MqttFactory> mqttFactoryReturning(MqttAsyncClient mqttClient) {
        var mqttFactory = mockStatic(MqttFactory.class);
        mqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                   .thenReturn(mqttClient);
        return mqttFactory;
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        InboundAggregator inboundAggregator() {
            var aggregator = mock(InboundAggregator.class);
            when(aggregator.inboundRecordFlux()).thenReturn(Flux.never());
            return aggregator;
        }

        @Bean
        MqttConfiguration mqttConfiguration() {
            return mock(MqttConfiguration.class);
        }

        @Bean
        BCryptPasswordEncoder passwordEncoder() {
            return mock(BCryptPasswordEncoder.class);
        }
    }
}
