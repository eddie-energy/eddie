package energy.eddie.aiida.services.secrets;

import energy.eddie.aiida.errors.SecretStoringException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.models.migration.Migration;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.repositories.AiidaMigrationRepository;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.repositories.MqttStreamingConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import static com.pivovarit.function.ThrowingFunction.sneaky;

@Component
public class PasswordMigrationRunner {
    private static final String MIGRATION_KEY = "V1_0__mqtt_password_migration";
    private static final String DESCRIPTION = "This migration removed plain text passwords from the database and migrated them to the Java KeyStore.";
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    private final AiidaMigrationRepository migrationRepository;
    private final DataSourceRepository dataSourceRepository;
    private final SecretsService secretService;
    private final MqttStreamingConfigRepository mqttStreamingConfigRepository;

    private int migratedDataSources = 0;
    private int migratedPermissions = 0;

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
    protected void migrate() {
        var migration = migrationRepository.findMigrationByMigrationKey(MIGRATION_KEY);

        if (migration.isPresent()) {
            return;
        }

        migrateDataSources();
        migrateMqttStreamingConfigs();
        LOGGER.info("Migrated plaintext passwords to Java Keystore from {} data source(s) and {} permission(s).",
                    migratedDataSources,
                    migratedPermissions);

        migrationRepository.save(new Migration(MIGRATION_KEY, DESCRIPTION));
    }

    private void migrateDataSources() {
        collectDataSources()
                .filter(MqttDataSource.class::isInstance)
                .map(sneaky(this::savePasswordToKeyStore))
                .forEach(this::replacePasswordsWithAlias);
    }

    private Stream<DataSource> collectDataSources() {
        return Stream.concat(dataSourceRepository.findAllByType(DataSourceType.INBOUND).stream(),
                             dataSourceRepository.findAllByType(DataSourceType.SINAPSI_ALFA).stream());
    }

    private MqttDataSource savePasswordToKeyStore(DataSource dataSource) throws SecretStoringException {
        var mqttDataSource = (MqttDataSource) dataSource;

        secretService.storeSecret(mqttDataSource.id(), SecretType.PASSWORD, mqttDataSource.password());
        LOGGER.debug("Stored plaintext secret for data source {} in java key store", mqttDataSource.id());
        migratedDataSources++;

        return mqttDataSource;
    }

    private void replacePasswordsWithAlias(MqttDataSource mqttDataSource) {
        if (mqttDataSource instanceof InboundDataSource inboundDataSource) {
            dataSourceRepository.save(inboundDataSource.copyWithAliasAsPassword());
        }
        if (mqttDataSource instanceof SinapsiAlfaDataSource sinapsiAlfaDataSource) {
            dataSourceRepository.save(sinapsiAlfaDataSource.copyWithAliasAsPassword());
        }
    }

    private void migrateMqttStreamingConfigs() {
        mqttStreamingConfigRepository.findAll()
                                     .stream()
                                     .map(sneaky(this::savePasswordToKeyStore))
                                     .forEach(this::replacePasswordsWithAlias);
    }

    private MqttStreamingConfig savePasswordToKeyStore(MqttStreamingConfig mqttStreamingConfig) throws SecretStoringException {
        secretService.storeSecret(mqttStreamingConfig.permissionId(),
                                  SecretType.PASSWORD,
                                  mqttStreamingConfig.password());
        LOGGER.debug("Stored plaintext secret for permission {} in java key store", mqttStreamingConfig.permissionId());
        migratedPermissions++;

        return mqttStreamingConfig;
    }

    private void replacePasswordsWithAlias(MqttStreamingConfig mqttStreamingConfig) {
        mqttStreamingConfigRepository.save(mqttStreamingConfig.copyWithAliasAsPassword());
    }
}
