// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.inbound;

import energy.eddie.aiida.dtos.datasource.mqtt.inbound.InboundDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.services.secrets.KeyStoreSecretsService;
import energy.eddie.aiida.services.secrets.SecretType;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InboundDataSourceTest {
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void constructor_setsPermissionAndInitialAccessCode() {
        // Given
        var permission = mock(Permission.class);
        when(permission.mqttStreamingConfig()).thenReturn(mock(MqttStreamingConfig.class));

        // When
        var dataSource = new InboundDataSource(dto(), USER_ID, permission, "initial-access-code");

        // Then
        assertEquals(permission, dataSource.permission());
        assertEquals("initial-access-code", dataSource.accessCode());
    }

    @Test
    void constructor_throwsNpe_whenMqttStreamingConfigMissing() {
        // Given
        var permission = mock(Permission.class);
        when(permission.mqttStreamingConfig()).thenReturn(null);
        var inboundDataSource = dto();

        // When / Then
        assertThrows(NullPointerException.class, () -> new InboundDataSource(inboundDataSource, USER_ID, permission));
    }

    @Test
    void postPersist_setsPasswordAndAccessCodeToKeystoreAliases() throws Exception {
        // Given
        var id = UUID.randomUUID();
        var config = mock(MqttStreamingConfig.class);
        when(config.username()).thenReturn(UUID.randomUUID());
        when(config.dataTopic()).thenReturn("data-topic");
        var permission = mock(Permission.class);
        when(permission.mqttStreamingConfig()).thenReturn(config);
        var dataSource = new InboundDataSource(dto(), USER_ID, permission, "initial-access-code");
        setId(dataSource, id);

        // When
        dataSource.postPersist();

        // Then
        assertEquals(KeyStoreSecretsService.alias(id, SecretType.PASSWORD), dataSource.password());
        assertEquals(KeyStoreSecretsService.alias(id, SecretType.API_KEY), dataSource.accessCode());
    }

    @Test
    void updateAccessCode_updatesField() {
        // Given
        var permission = mock(Permission.class);
        when(permission.mqttStreamingConfig()).thenReturn(mock(MqttStreamingConfig.class));
        var dataSource = new InboundDataSource(dto(), USER_ID, permission, "initial-access-code");

        // When
        dataSource.updateAccessCode("new-access-code");

        // Then
        assertEquals("new-access-code", dataSource.accessCode());
    }

    @Test
    void schemas_returnsEmptySet_whenDataNeedNull() {
        // Given
        var permission = mock(Permission.class);
        when(permission.mqttStreamingConfig()).thenReturn(mock(MqttStreamingConfig.class));
        when(permission.dataNeed()).thenReturn(null);
        var dataSource = new InboundDataSource(dto(), USER_ID, permission, "initial-access-code");

        // When / Then
        assertEquals(Set.of(), dataSource.schemas());
    }

    @Test
    void schemas_returnsDataNeedSchemas_whenPresent() {
        // Given
        var dataNeed = mock(AiidaLocalDataNeed.class);
        when(dataNeed.schemas()).thenReturn(Set.of(AiidaSchema.SMART_METER_P1_RAW));
        var permission = mock(Permission.class);
        when(permission.mqttStreamingConfig()).thenReturn(mock(MqttStreamingConfig.class));
        when(permission.dataNeed()).thenReturn(dataNeed);
        var dataSource = new InboundDataSource(dto(), USER_ID, permission, "initial-access-code");

        // When / Then
        assertEquals(Set.of(AiidaSchema.SMART_METER_P1_RAW), dataSource.schemas());
    }

    @Test
    void acknowledgementTopic_returnsNull_whenConfigIsTransientAndUnset() throws Exception {
        // Given: the no-arg constructor is used by JPA when hydrating an entity from the database;
        // the transient `config` field is never populated in that path.
        var constructor = InboundDataSource.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var dataSource = constructor.newInstance();

        // When / Then
        assertNull(dataSource.acknowledgementTopic());
    }

    @Test
    void acknowledgementTopic_returnsTopic_whenConfigPresent() {
        // Given
        var config = mock(MqttStreamingConfig.class);
        when(config.acknowledgementTopic()).thenReturn("ack-topic");
        var permission = mock(Permission.class);
        when(permission.mqttStreamingConfig()).thenReturn(config);
        var dataSource = new InboundDataSource(dto(), USER_ID, permission, "initial-access-code");

        // When / Then
        assertEquals("ack-topic", dataSource.acknowledgementTopic());
    }

    @Test
    void builder_throwsNpe_whenPermissionUserIdMissing() {
        // Given
        var permission = mock(Permission.class);

        // When / Then
        assertThrows(NullPointerException.class, () -> new InboundDataSource.Builder(permission));
    }

    @Test
    void builder_build_createsInboundDataSourceLinkedToPermission() {
        // Given
        var permissionId = UUID.randomUUID();
        var dataNeed = mock(AiidaLocalDataNeed.class);
        when(dataNeed.asset()).thenReturn(AiidaAsset.SUBMETER);
        var permission = mock(Permission.class);
        when(permission.userId()).thenReturn(USER_ID);
        when(permission.id()).thenReturn(permissionId);
        when(permission.dataNeed()).thenReturn(dataNeed);
        when(permission.mqttStreamingConfig()).thenReturn(mock(MqttStreamingConfig.class));

        // When
        var dataSource = new InboundDataSource.Builder(permission).build();

        // Then
        assertEquals(permission, dataSource.permission());
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
