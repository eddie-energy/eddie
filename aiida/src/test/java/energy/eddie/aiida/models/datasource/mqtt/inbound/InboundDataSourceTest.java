// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.inbound;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.datasource.mqtt.inbound.InboundDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.services.secrets.KeyStoreSecretsService;
import energy.eddie.aiida.services.secrets.SecretType;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class InboundDataSourceTest {
    private static final UUID DATA_SOURCE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String STREAMING_HOST = "tcp://aiida-broker:1883";
    private static final String STREAMING_PASSWORD = "streaming-password";
    private static final String STREAMING_TOPIC = "aiida/streaming";
    private static final String ACKNOWLEDGEMENT_TOPIC = "aiida/ack";
    private static final String ACCESS_CODE = "access-code";
    private static final String CLIENT_HOST = "tcp://client-broker:1883";
    private static final String CLIENT_USERNAME = "client-user";
    private static final String CLIENT_PASSWORD = "client-password";
    private static final String CLIENT_TOPIC = "client/inbound";
    private static final String SERVER_INTERNAL_HOST = "tcp://internal-broker:1883";
    private static final String SERVER_EXTERNAL_HOST = "ssl://external-broker:8883";
    private static final String SERVER_PASSWORD = "server-password";
    private static final String SERVER_PASSWORD_HASH = "encoded-server-credential";

    private Permission permission;
    private InboundDataSource dataSource;

    @BeforeEach
    void setUp() {
        var dto = mock(InboundDataSourceDto.class);
        var streamingConfig = mock(MqttStreamingConfig.class);
        permission = mock(Permission.class);

        when(dto.id()).thenReturn(DATA_SOURCE_ID);
        when(streamingConfig.serverUri()).thenReturn(STREAMING_HOST);
        when(streamingConfig.username()).thenReturn(DATA_SOURCE_ID);
        when(streamingConfig.password()).thenReturn(STREAMING_PASSWORD);
        when(streamingConfig.dataTopic()).thenReturn(STREAMING_TOPIC);
        when(streamingConfig.acknowledgementTopic()).thenReturn(ACKNOWLEDGEMENT_TOPIC);
        when(permission.mqttStreamingConfig()).thenReturn(streamingConfig);

        dataSource = new InboundDataSource(dto, USER_ID, permission, ACCESS_CODE);
        dataSource.createMqttUser();
        dataSource.createAccessControlEntry();
    }

    @Test
    void constructor_initializesRestProvisioningAndPermissionDetails() {
        assertThat(dataSource.inboundProvisioningType()).isEqualTo(InboundProvisioningType.REST_BEARER);
        assertThat(dataSource.accessCode()).isEqualTo(ACCESS_CODE);
        assertThat(dataSource.permission()).isSameAs(permission);
        assertThat(dataSource.acknowledgementTopic()).isEqualTo(ACKNOWLEDGEMENT_TOPIC);
        assertThatThrownBy(dataSource::provisioningConnection)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Provisioning MQTT configuration is not configured");
    }

    @Test
    void establishClientModeConnection_activatesClientModeAndStoresSuppliedConnection() {
        var result = dataSource.establishClientModeConnection(
                CLIENT_HOST,
                CLIENT_USERNAME,
                CLIENT_PASSWORD,
                CLIENT_TOPIC
        );

        assertThat(dataSource.inboundProvisioningType()).isEqualTo(InboundProvisioningType.MQTT_CLIENT);
        assertThat(result.host()).isEqualTo(CLIENT_HOST);
        assertThat(result.username()).isEqualTo(CLIENT_USERNAME);
        assertThat(result.password()).isEqualTo(CLIENT_PASSWORD);
        assertThat(result.topic()).isEqualTo(CLIENT_TOPIC);
        assertThat(dataSource.provisioningConnection().internalHost()).isEqualTo(CLIENT_HOST);
        assertThat(dataSource.provisioningConnection().username()).isEqualTo(CLIENT_USERNAME);
        assertThat(dataSource.provisioningConnection().password()).isEqualTo(CLIENT_PASSWORD);
        assertThat(dataSource.provisioningTopicOrThrow()).isEqualTo(CLIENT_TOPIC);
    }

    @Test
    void establishServerModeConnection_returnsPlaintextButStoresEncodedPassword() {
        var mqttConfiguration = mock(MqttConfiguration.class);
        var passwordEncoder = mock(BCryptPasswordEncoder.class);
        when(mqttConfiguration.internalHost()).thenReturn(SERVER_INTERNAL_HOST);
        when(mqttConfiguration.externalHost()).thenReturn(SERVER_EXTERNAL_HOST);
        when(passwordEncoder.encode(SERVER_PASSWORD)).thenReturn(SERVER_PASSWORD_HASH);

        var result = dataSource.establishServerModeConnection(
                mqttConfiguration,
                passwordEncoder,
                SERVER_PASSWORD
        );

        assertThat(dataSource.inboundProvisioningType()).isEqualTo(InboundProvisioningType.MQTT_SERVER);
        assertThat(result.host()).isEqualTo(SERVER_EXTERNAL_HOST);
        assertThatCode(() -> UUID.fromString(result.username())).doesNotThrowAnyException();
        assertThat(result.password()).isEqualTo(SERVER_PASSWORD);
        assertThat(result.topic()).isEqualTo("aiida/" + result.username() + "/inboundData");
        assertThat(dataSource.provisioningConnection().internalHost()).isEqualTo(SERVER_INTERNAL_HOST);
        assertThat(dataSource.provisioningConnection().username()).isEqualTo(result.username());
        assertThat(dataSource.provisioningConnection().password()).isEqualTo(SERVER_PASSWORD_HASH);
        assertThat(dataSource.provisioningTopicOrThrow()).isEqualTo(result.topic());
        verify(passwordEncoder).encode(SERVER_PASSWORD);
    }

    @Test
    void mqttClient_serializationExposesConnectionDetailsWithoutPassword() {
        var credentials = dataSource.establishClientModeConnection(
                CLIENT_HOST,
                CLIENT_USERNAME,
                CLIENT_PASSWORD,
                CLIENT_TOPIC
        );

        var json = ObjectMapperCreatorUtil.mapper().writeValueAsString(dataSource);

        assertThat(credentials.password()).isEqualTo(CLIENT_PASSWORD);
        assertThat(json)
                .contains("\"provisioningType\":\"MQTT_CLIENT\"")
                .contains("\"mqttProvisioningConfig\"")
                .contains("\"externalHost\":\"" + CLIENT_HOST + "\"")
                .contains("\"username\":\"" + CLIENT_USERNAME + "\"")
                .contains("\"topic\":\"" + CLIENT_TOPIC + "\"")
                .doesNotContain("\"password\"")
                .doesNotContain(CLIENT_PASSWORD);
    }

    @Test
    void mqttServer_serializationExposesConnectionDetailsWithoutPassword() {
        var mqttConfiguration = mock(MqttConfiguration.class);
        var passwordEncoder = mock(BCryptPasswordEncoder.class);
        when(mqttConfiguration.internalHost()).thenReturn(SERVER_INTERNAL_HOST);
        when(mqttConfiguration.externalHost()).thenReturn(SERVER_EXTERNAL_HOST);
        when(passwordEncoder.encode(SERVER_PASSWORD)).thenReturn(SERVER_PASSWORD_HASH);
        var credentials = dataSource.establishServerModeConnection(
                mqttConfiguration,
                passwordEncoder,
                SERVER_PASSWORD
        );

        var json = ObjectMapperCreatorUtil.mapper().writeValueAsString(dataSource);

        assertThat(credentials.password()).isEqualTo(SERVER_PASSWORD);
        assertThat(json)
                .contains("\"provisioningType\":\"MQTT_SERVER\"")
                .contains("\"externalHost\":\"" + SERVER_EXTERNAL_HOST + "\"")
                .contains("\"topic\":\"aiida/" + credentials.username() + "/inboundData\"")
                .doesNotContain("\"password\"")
                .doesNotContain(SERVER_PASSWORD)
                .doesNotContain(SERVER_PASSWORD_HASH)
                .containsPattern("\"username\":\"[0-9a-f-]{36}\"");
    }

    @Test
    void changeInboundProvisioningType_toRest_removesMqttProvisioning() {
        dataSource.establishClientModeConnection(
                CLIENT_HOST,
                CLIENT_USERNAME,
                CLIENT_PASSWORD,
                CLIENT_TOPIC
        );

        dataSource.changeInboundProvisioningType(InboundProvisioningType.REST_API_TOKEN);

        assertThat(dataSource.inboundProvisioningType()).isEqualTo(InboundProvisioningType.REST_API_TOKEN);
        assertThatThrownBy(dataSource::provisioningConnection)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Provisioning MQTT configuration is not configured");
        assertThatThrownBy(dataSource::provisioningTopicOrThrow)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Provisioning MQTT configuration is not configured");
    }

    @Test
    void constructor_throwsNpe_whenMqttStreamingConfigMissing() {
        var permissionWithoutConfig = mock(Permission.class);

        assertThrows(
                NullPointerException.class,
                () -> new InboundDataSource(dto(), USER_ID, permissionWithoutConfig)
        );
    }

    @Test
    void postPersist_setsPasswordAndAccessCodeToKeystoreAliases() throws Exception {
        var id = UUID.randomUUID();
        var config = mock(MqttStreamingConfig.class);
        when(config.username()).thenReturn(UUID.randomUUID());
        when(config.dataTopic()).thenReturn("data-topic");
        var owningPermission = mock(Permission.class);
        when(owningPermission.mqttStreamingConfig()).thenReturn(config);
        var inboundDataSource = new InboundDataSource(dto(), USER_ID, owningPermission, "initial-access-code");
        setId(inboundDataSource, id);

        inboundDataSource.postPersist();

        assertEquals(KeyStoreSecretsService.alias(id, SecretType.PASSWORD), inboundDataSource.password());
        assertEquals(KeyStoreSecretsService.alias(id, SecretType.API_KEY), inboundDataSource.accessCode());
    }

    @Test
    void updateAccessCode_updatesField() {
        dataSource.updateAccessCode("new-access-code");

        assertEquals("new-access-code", dataSource.accessCode());
    }

    @Test
    void schemas_returnsEmptySet_whenDataNeedNull() {
        when(permission.dataNeed()).thenReturn(null);

        assertEquals(Set.of(), dataSource.schemas());
    }

    @Test
    void schemas_returnsDataNeedSchemas_whenPresent() {
        var dataNeed = mock(AiidaLocalDataNeed.class);
        when(dataNeed.schemas()).thenReturn(Set.of(AiidaSchema.SMART_METER_P1_RAW));
        when(permission.dataNeed()).thenReturn(dataNeed);

        assertEquals(Set.of(AiidaSchema.SMART_METER_P1_RAW), dataSource.schemas());
    }

    @Test
    void builder_throwsNpe_whenPermissionUserIdMissing() {
        var permissionWithoutUser = mock(Permission.class);

        assertThrows(NullPointerException.class, () -> new InboundDataSource.Builder(permissionWithoutUser));
    }

    @Test
    void builder_build_createsInboundDataSourceLinkedToPermission() {
        var permissionId = UUID.randomUUID();
        var dataNeed = mock(AiidaLocalDataNeed.class);
        when(dataNeed.asset()).thenReturn(AiidaAsset.SUBMETER);
        var owningPermission = mock(Permission.class);
        when(owningPermission.userId()).thenReturn(USER_ID);
        when(owningPermission.id()).thenReturn(permissionId);
        when(owningPermission.dataNeed()).thenReturn(dataNeed);
        when(owningPermission.mqttStreamingConfig()).thenReturn(mock(MqttStreamingConfig.class));

        var inboundDataSource = new InboundDataSource.Builder(owningPermission).build();

        assertEquals(owningPermission, inboundDataSource.permission());
    }

    private static InboundDataSourceDto dto() {
        return new InboundDataSourceDto(AiidaAsset.SUBMETER, UUID.randomUUID());
    }

    private static void setId(InboundDataSource dataSource, UUID id) throws Exception {
        Field field = DataSource.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(dataSource, id);
    }
}
