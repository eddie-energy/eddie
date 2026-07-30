// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.secrets;

import energy.eddie.aiida.errors.SecretStoringException;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.models.migration.Migration;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.repositories.AiidaMigrationRepository;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.repositories.MqttStreamingConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.aiida.services.secrets.KeyStoreSecretsService.alias;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordMigrationRunnerTest {
    private static final String MIGRATION_KEY = "V1_0__mqtt_password_migration";

    @Mock
    private AiidaMigrationRepository migrationRepository;
    @Mock
    private DataSourceRepository dataSourceRepository;
    @Mock
    private KeyStoreSecretsService secretService;
    @Mock
    private MqttStreamingConfigRepository mqttStreamingConfigRepository;

    @Test
    void doesNothingWhenMigrationAlreadyRan() throws SecretStoringException {
        when(migrationRepository.findMigrationByMigrationKey(MIGRATION_KEY)).thenReturn(Optional.of(new Migration(MIGRATION_KEY, "already ran")));

        runner().migrate();

        verifyNoInteractions(dataSourceRepository, secretService, mqttStreamingConfigRepository);
        verify(migrationRepository, never()).save(any());
    }

    @Test
    void migratesInboundDataSourcePasswordAndAccessCode() throws SecretStoringException {
        var id = UUID.randomUUID();
        var inboundDataSource = mock(InboundDataSource.class);
        when(inboundDataSource.id()).thenReturn(id);
        when(inboundDataSource.password()).thenReturn("plaintext-password");
        when(inboundDataSource.accessCode()).thenReturn("plaintext-access-code");

        when(migrationRepository.findMigrationByMigrationKey(MIGRATION_KEY)).thenReturn(Optional.empty());
        when(dataSourceRepository.findAllByType(DataSourceType.INBOUND)).thenReturn(List.of(inboundDataSource));
        when(dataSourceRepository.findAllByType(DataSourceType.SINAPSI_ALFA)).thenReturn(List.of());
        when(mqttStreamingConfigRepository.findAll()).thenReturn(List.of());

        runner().migrate();

        var inOrder = inOrder(secretService, dataSourceRepository, inboundDataSource);
        inOrder.verify(secretService).storeSecret(id, SecretType.PASSWORD, "plaintext-password");
        inOrder.verify(secretService).storeSecret(id, SecretType.API_KEY, "plaintext-access-code");
        inOrder.verify(inboundDataSource).updatePassword(alias(id, SecretType.PASSWORD));
        inOrder.verify(inboundDataSource).updateAccessCode(alias(id, SecretType.API_KEY));
        inOrder.verify(dataSourceRepository).save(inboundDataSource);

        verify(migrationRepository).save(argThat(migration -> true));
    }

    @Test
    void migratesSinapsiAlfaPasswordOnlyWithoutTouchingAccessCode() throws SecretStoringException {
        var id = UUID.randomUUID();
        var sinapsiAlfaDataSource = mock(SinapsiAlfaDataSource.class);
        when(sinapsiAlfaDataSource.id()).thenReturn(id);
        when(sinapsiAlfaDataSource.password()).thenReturn("plaintext-password");

        when(migrationRepository.findMigrationByMigrationKey(MIGRATION_KEY)).thenReturn(Optional.empty());
        when(dataSourceRepository.findAllByType(DataSourceType.INBOUND)).thenReturn(List.of());
        when(dataSourceRepository.findAllByType(DataSourceType.SINAPSI_ALFA)).thenReturn(List.of(sinapsiAlfaDataSource));
        when(mqttStreamingConfigRepository.findAll()).thenReturn(List.of());

        runner().migrate();

        verify(secretService).storeSecret(id, SecretType.PASSWORD, "plaintext-password");
        verify(secretService, never()).storeSecret(eq(id), eq(SecretType.API_KEY), any());
        verify(sinapsiAlfaDataSource).updatePassword(alias(id, SecretType.PASSWORD));
        verify(dataSourceRepository).save(sinapsiAlfaDataSource);
    }

    @Test
    void migratesMqttStreamingConfigPassword() throws SecretStoringException {
        var permissionId = UUID.randomUUID();
        var config = mock(MqttStreamingConfig.class);
        when(config.permissionId()).thenReturn(permissionId);
        when(config.password()).thenReturn("plaintext-password");

        when(migrationRepository.findMigrationByMigrationKey(MIGRATION_KEY)).thenReturn(Optional.empty());
        when(dataSourceRepository.findAllByType(any())).thenReturn(List.of());
        when(mqttStreamingConfigRepository.findAll()).thenReturn(List.of(config));

        runner().migrate();

        verify(secretService).storeSecret(permissionId, SecretType.PASSWORD, "plaintext-password");
        verify(config).updatePassword(alias(permissionId, SecretType.PASSWORD));
        verify(mqttStreamingConfigRepository).save(config);
    }

    @Test
    void failureMidBatchLeavesMigrationGuardRowUnsavedAndPropagatesTheFailure() throws SecretStoringException {
        var firstId = UUID.randomUUID();
        var secondId = UUID.randomUUID();
        var firstDataSource = mock(InboundDataSource.class);
        var secondDataSource = mock(InboundDataSource.class);
        when(firstDataSource.id()).thenReturn(firstId);
        when(firstDataSource.password()).thenReturn("first-password");
        when(firstDataSource.accessCode()).thenReturn("first-access-code");
        when(secondDataSource.id()).thenReturn(secondId);
        when(secondDataSource.password()).thenReturn("second-password");

        when(migrationRepository.findMigrationByMigrationKey(MIGRATION_KEY)).thenReturn(Optional.empty());
        when(dataSourceRepository.findAllByType(DataSourceType.INBOUND)).thenReturn(List.of(firstDataSource, secondDataSource));
        when(dataSourceRepository.findAllByType(DataSourceType.SINAPSI_ALFA)).thenReturn(List.of());
        lenient().doThrow(new SecretStoringException(secondId, new RuntimeException("keystore unavailable")))
                .when(secretService).storeSecret(secondId, SecretType.PASSWORD, "second-password");

        assertThatThrownBy(() -> runner().migrate()).isInstanceOf(SecretStoringException.class);

        // the guard row must never be written on a failed run, otherwise a subsequent
        // restart would skip the migration entirely, leaving the second row un-migrated forever
        verify(migrationRepository, never()).save(any());
    }

    private PasswordMigrationRunner runner() {
        return new PasswordMigrationRunner(migrationRepository, dataSourceRepository, secretService, mqttStreamingConfigRepository);
    }
}
