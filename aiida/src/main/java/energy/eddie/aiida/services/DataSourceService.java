package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.config.datasource.it.SinapsiAlfaConfiguration;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.dtos.datasource.DataSourceSecretsDto;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.dtos.events.DataSourceDeletionEvent;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.datasource.DataSourceSecretGenerationNotSupportedException;
import energy.eddie.aiida.errors.datasource.modbus.ModbusConnectionException;
import energy.eddie.aiida.errors.datasource.mqtt.it.SinapsiAlflaEmptyConfigException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.SecretGenerator;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.aiida.repositories.DataSourceRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class DataSourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceService.class);

    private final DataSourceRepository repository;
    private final AiidaEventPublisher aiidaEventPublisher;
    private final Aggregator aggregator;
    private final AuthService authService;
    private final Set<DataSourceAdapter<? extends DataSource>> dataSourceAdapters = new HashSet<>();
    private final MqttConfiguration mqttConfiguration;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final SinapsiAlfaConfiguration sinapsiAlfaConfiguration;

    @Autowired
    public DataSourceService(
            DataSourceRepository repository,
            Aggregator aggregator,
            AuthService authService,
            MqttConfiguration mqttConfiguration,
            ObjectMapper objectMapper,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            SinapsiAlfaConfiguration sinapsiAlfaConfiguration,
            AiidaEventPublisher aiidaEventPublisher
    ) {
        this.repository = repository;
        this.aggregator = aggregator;
        this.authService = authService;
        this.mqttConfiguration = mqttConfiguration;
        this.objectMapper = objectMapper;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.sinapsiAlfaConfiguration = sinapsiAlfaConfiguration;
        this.aiidaEventPublisher = aiidaEventPublisher;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void startDataSources() {
        var dataSources = repository.findAll();

        for (var dataSource : dataSources) {
            startDataSource(dataSource);
        }
    }

    public void startDataSource(DataSource dataSource) throws ModbusConnectionException {
        var dataSourceAdapter = DataSourceAdapter.create(dataSource, objectMapper, mqttConfiguration);
        dataSourceAdapters.add(dataSourceAdapter);

        if (dataSource.enabled()) {
            aggregator.addNewDataSourceAdapter(dataSourceAdapter);
        }
    }

    public DataSource dataSourceByIdOrThrow(UUID dataSourceId) throws DataSourceNotFoundException {
        return repository.findById(dataSourceId)
                         .orElseThrow(() -> new DataSourceNotFoundException(dataSourceId));
    }

    public List<DataSourceType> getOutboundDataSourceTypes() {
        return Arrays.stream(DataSourceType.values())
                     .filter(this::isOutboundDataSourceType)
                     .toList();
    }

    public List<DataSource> getInboundDataSources() throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();

        return repository.findByUserId(currentUserId)
                         .stream()
                         .filter(dataSource -> isInboundDataSourceType(dataSource.dataSourceType()))
                         .toList();
    }

    public List<DataSource> getOutboundDataSources() throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();

        return repository.findByUserId(currentUserId)
                         .stream()
                         .filter(dataSource -> isOutboundDataSourceType(dataSource.dataSourceType()))
                         .toList();
    }

    @Transactional
    public DataSourceSecretsDto addDataSource(DataSourceDto dto) throws InvalidUserException, SinapsiAlflaEmptyConfigException {
        var currentUserId = authService.getCurrentUserId();
        var plaintextPassword = "";

        var dataSource = DataSource.createFromDto(dto, currentUserId);
        LOGGER.info("Created new data source ({}) of type {}", dataSource.id(), dataSource.dataSourceType());

        if (dataSource instanceof MqttDataSource mqttDataSource) {
            if (mqttDataSource instanceof SinapsiAlfaDataSource sinapsiAlfaDataSource && dto instanceof SinapsiAlfaDataSourceDto sinapsiAlfaDataSourceDto) {
                sinapsiAlfaDataSource.generateMqttSettings(sinapsiAlfaConfiguration,
                                                           sinapsiAlfaDataSourceDto.activationKey());
                LOGGER.info("Generated MQTT settings for Sinapsi Alfa data source ({})", dataSource.id());
            } else {
                plaintextPassword = SecretGenerator.generate();
                mqttDataSource.generateMqttSettings(mqttConfiguration, bCryptPasswordEncoder, plaintextPassword);
                LOGGER.info("Generated MQTT settings for data source ({})", dataSource.id());
            }
        }

        repository.save(dataSource);
        startDataSource(dataSource);

        return new DataSourceSecretsDto(dataSource.id(), plaintextPassword);
    }

    @Transactional
    public void deleteDataSource(UUID dataSourceId) {
        findDataSourceAdapter(dataSourceId).ifPresentOrElse(
                this::closeDataSourceAdapter,
                () -> LOGGER.warn(
                        "Tried to close DataSourceAdapter but could not be found for data source ({}).",
                        dataSourceId));

        repository.findById(dataSourceId)
                  .ifPresentOrElse(
                          dataSource -> {
                              var permissionIds = dataSource.permissions()
                                                            .stream()
                                                            .map(Permission::id)
                                                            .collect(Collectors.toSet());

                              aiidaEventPublisher.publishEvent(new DataSourceDeletionEvent(permissionIds));

                              var dataSourceName = dataSource.name();
                              repository.delete(dataSource);
                              LOGGER.info("Deleted data source {} ({})", dataSourceName, dataSourceId);
                          },
                          () -> LOGGER.warn("Tried to delete data source ({}) but it could not found be found.",
                                            dataSourceId));
    }

    @Transactional
    public DataSource updateDataSource(DataSourceDto dto) throws ModbusConnectionException, DataSourceNotFoundException {
        var dataSource = repository.findById(dto.id())
                                   .orElseThrow(() -> new DataSourceNotFoundException(dto.id()));

        dataSource.update(dto);

        findDataSourceAdapter(dataSource.id()).ifPresentOrElse(
                adapter -> updateDataSourceAdapterState(
                        adapter,
                        dataSource,
                        dataSource.enabled(),
                        dto.enabled()),
                () -> startDataSource(dataSource));

        LOGGER.debug("Updated data source {} with content {}", dto.id(), dto);

        return dataSource;
    }

    @Transactional
    public void updateEnabledState(UUID dataSourceId, boolean enabled) throws DataSourceNotFoundException {
        var dataSource = repository.findById(dataSourceId)
                                   .orElseThrow(() -> new DataSourceNotFoundException(dataSourceId));

        var currentEnabledState = dataSource.enabled();
        dataSource.setEnabled(enabled);

        findDataSourceAdapter(dataSourceId).ifPresentOrElse(
                adapter -> updateDataSourceAdapterState(
                        adapter,
                        dataSource,
                        currentEnabledState,
                        enabled),
                () -> startDataSource(dataSource));

        LOGGER.debug("Updated enabled state of data source {} to {}", dataSourceId, enabled);
    }

    @Transactional
    public DataSourceSecretsDto regenerateSecrets(UUID dataSourceId) throws DataSourceNotFoundException, DataSourceSecretGenerationNotSupportedException {
        var dataSource = repository.findById(dataSourceId)
                                   .orElseThrow(() -> new DataSourceNotFoundException(dataSourceId));

        var mqttDataSource = castToMqttDataSourceIfSecretRegenerationIsSupported(dataSource);
        var dataSourceSecrets = new DataSourceSecretsDto(dataSourceId, SecretGenerator.generate());

        mqttDataSource.setPassword(dataSourceSecrets.plaintextPassword(), bCryptPasswordEncoder);

        findDataSourceAdapter(dataSourceId).ifPresentOrElse(
                adapter -> updateDataSourceAdapterState(
                        adapter,
                        dataSource,
                        dataSource.enabled(),
                        dataSource.enabled()),
                () -> startDataSource(dataSource));

        LOGGER.debug("Regenerated secrets for data source {}", dataSourceId);

        return dataSourceSecrets;
    }

    public Optional<DataSourceAdapter<? extends DataSource>> findDataSourceAdapter(UUID dataSourceId) {
        return findDataSourceAdapter(adapter -> adapter.dataSource().id().equals(dataSourceId));
    }

    public Optional<DataSourceAdapter<? extends DataSource>> findDataSourceAdapter(Predicate<DataSourceAdapter<? extends DataSource>> predicate) {
        return dataSourceAdapters.stream().filter(predicate).findFirst();
    }

    public InboundDataSource createInboundDataSource(Permission permission) {
        var inboundDataSource = new InboundDataSource.Builder(permission).build();

        LOGGER.info("Created inbound data source {}", inboundDataSource.id());
        return repository.save(inboundDataSource);
    }

    private boolean isOutboundDataSourceType(DataSourceType dataSourceType) {
        return !isInboundDataSourceType(dataSourceType);
    }

    private boolean isInboundDataSourceType(DataSourceType dataSourceType) {
        return dataSourceType == DataSourceType.INBOUND;
    }

    private void closeDataSourceAdapter(DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        aggregator.removeDataSourceAdapter(dataSourceAdapter);
        dataSourceAdapters.remove(dataSourceAdapter);
    }

    private MqttDataSource castToMqttDataSourceIfSecretRegenerationIsSupported(DataSource dataSource) throws DataSourceSecretGenerationNotSupportedException {
        if (dataSource instanceof MqttDataSource mqttDataSource) {
            if (mqttDataSource.dataSourceType() == DataSourceType.INBOUND ||
                mqttDataSource.dataSourceType() == DataSourceType.SINAPSI_ALFA) {
                throw new DataSourceSecretGenerationNotSupportedException(dataSource.dataSourceType());
            }

            return mqttDataSource;
        }

        throw new DataSourceSecretGenerationNotSupportedException(dataSource.dataSourceType());
    }

    private void updateDataSourceAdapterState(
            DataSourceAdapter<? extends DataSource> dataSourceAdapter,
            DataSource dataSource,
            boolean currentEnabledState,
            boolean newEnabledState
    ) {
        if (newEnabledState == currentEnabledState) {
            if (newEnabledState) {
                closeDataSourceAdapter(dataSourceAdapter);
                startDataSource(dataSource);
            }
        } else if (newEnabledState) {
            startDataSource(dataSource);
        } else {
            closeDataSourceAdapter(dataSourceAdapter);
        }
    }
}
