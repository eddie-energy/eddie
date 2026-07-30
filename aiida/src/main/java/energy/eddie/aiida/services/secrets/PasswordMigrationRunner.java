// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.secrets;

import energy.eddie.aiida.errors.SecretStoringException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.migration.Migration;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.repositories.AiidaMigrationRepository;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.repositories.MqttStreamingConfigRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import static energy.eddie.aiida.services.secrets.KeyStoreSecretsService.alias;

@Component
public class PasswordMigrationRunner {
    private static final String MIGRATION_KEY = "V1_0__mqtt_password_migration";
    private static final String DESCRIPTION = "This migration removed plain text passwords from the database and migrated them to the Java KeyStore.";
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    private final AiidaMigrationRepository migrationRepository;
    private final DataSourceRepository dataSourceRepository;
    private final SecretsService secretService;
    private final MqttStreamingConfigRepository mqttStreamingConfigRepository;

    public PasswordMigrationRunner(
            AiidaMigrationRepository migrationRepository,
            DataSourceRepository dataSourceRepository,
            KeyStoreSecretsService secretService,
            MqttStreamingConfigRepository mqttStreamingConfigRepository
    ) {
        this.migrationRepository = migrationRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.secretService = secretService;
        this.mqttStreamingConfigRepository = mqttStreamingConfigRepository;
    }

    @EventListener(ContextRefreshedEvent.class)
    @Transactional(rollbackOn = SecretStoringException.class)
    protected void migrate() throws SecretStoringException {
        var migration = migrationRepository.findMigrationByMigrationKey(MIGRATION_KEY);

        if (migration.isPresent()) {
            return;
        }

        var migratedDataSources = migrateDataSources();
        var migratedPermissions = migrateMqttStreamingConfigs();
        LOGGER.info("Migrated plaintext passwords to Java Keystore from {} data source(s) and {} permission(s).",
                    migratedDataSources,
                    migratedPermissions);

        migrationRepository.save(new Migration(MIGRATION_KEY, DESCRIPTION));
    }

    private int migrateDataSources() throws SecretStoringException {
        var mqttDataSources = collectDataSources()
                .filter(MqttDataSource.class::isInstance)
                .map(MqttDataSource.class::cast)
                .toList();

        for (var mqttDataSource : mqttDataSources) {
            savePasswordToKeyStore(mqttDataSource);
        }

        mqttDataSources.forEach(this::replacePasswordWithAlias);

        return mqttDataSources.size();
    }

    private Stream<DataSource> collectDataSources() {
        return Stream.concat(dataSourceRepository.findAllByType(DataSourceType.INBOUND).stream(),
                             dataSourceRepository.findAllByType(DataSourceType.SINAPSI_ALFA).stream());
    }

    private void savePasswordToKeyStore(MqttDataSource mqttDataSource) throws SecretStoringException {
        secretService.storeSecret(mqttDataSource.id(), SecretType.PASSWORD, mqttDataSource.password());

        if (mqttDataSource instanceof InboundDataSource inboundDataSource) {
            secretService.storeSecret(inboundDataSource.id(), SecretType.API_KEY, inboundDataSource.accessCode());
        }

        LOGGER.debug("Stored plaintext secret(s) for data source {} in java key store", mqttDataSource.id());
    }

    private void replacePasswordWithAlias(MqttDataSource mqttDataSource) {
        mqttDataSource.updatePassword(alias(mqttDataSource.id(), SecretType.PASSWORD));

        if (mqttDataSource instanceof InboundDataSource inboundDataSource) {
            inboundDataSource.updateAccessCode(alias(inboundDataSource.id(), SecretType.API_KEY));
        }

        dataSourceRepository.save(mqttDataSource);
    }

    private int migrateMqttStreamingConfigs() throws SecretStoringException {
        var mqttStreamingConfigs = mqttStreamingConfigRepository.findAll();

        for (var mqttStreamingConfig : mqttStreamingConfigs) {
            savePasswordToKeyStore(mqttStreamingConfig);
        }

        mqttStreamingConfigs.forEach(this::replacePasswordWithAlias);

        return mqttStreamingConfigs.size();
    }

    private void savePasswordToKeyStore(MqttStreamingConfig mqttStreamingConfig) throws SecretStoringException {
        secretService.storeSecret(mqttStreamingConfig.permissionId(),
                                  SecretType.PASSWORD,
                                  mqttStreamingConfig.password());
        LOGGER.debug("Stored plaintext secret for permission {} in java key store", mqttStreamingConfig.permissionId());
    }

    private void replacePasswordWithAlias(MqttStreamingConfig mqttStreamingConfig) {
        mqttStreamingConfig.updatePassword(alias(mqttStreamingConfig.permissionId(), SecretType.PASSWORD));
        mqttStreamingConfigRepository.save(mqttStreamingConfig);
    }
}
