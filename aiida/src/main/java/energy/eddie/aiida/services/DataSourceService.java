package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.config.datasource.it.SinapsiAlfaConfig;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.dtos.datasource.DataSourceSecretsDto;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.errors.DataSourceNotFoundException;
import energy.eddie.aiida.errors.InvalidUserException;
import energy.eddie.aiida.errors.ModbusConnectionException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.SecretGenerator;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.repositories.DataSourceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;

@Service
public class DataSourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceService.class);

    private final DataSourceRepository repository;
    private final Aggregator aggregator;
    private final AuthService authService;
    private final Set<DataSourceAdapter<? extends DataSource>> dataSourceAdapters = new HashSet<>();
    private final MqttConfiguration mqttConfiguration;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final SinapsiAlfaConfig sinapsiAlfaConfig;

    @Autowired
    public DataSourceService(
            DataSourceRepository repository,
            Aggregator aggregator,
            AuthService authService,
            MqttConfiguration mqttConfiguration,
            ObjectMapper objectMapper,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            SinapsiAlfaConfig sinapsiAlfaConfig
    ) {
        this.repository = repository;
        this.aggregator = aggregator;
        this.authService = authService;
        this.mqttConfiguration = mqttConfiguration;
        this.objectMapper = objectMapper;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.sinapsiAlfaConfig = sinapsiAlfaConfig;
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

    public Optional<DataSource> dataSourceById(UUID dataSourceId) {
        return repository.findById(dataSourceId);
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

    public DataSourceSecretsDto addDataSource(DataSourceDto dto) throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();
        var plaintextPassword = SecretGenerator.generate();

        var dataSource = DataSource.createFromDto(dto, currentUserId);
        repository.save(dataSource); // This save generates the datasource ID

        if (dataSource instanceof MqttDataSource mqttDataSource) {
            if (mqttDataSource instanceof SinapsiAlfaDataSource sinapsiAlfaDataSource && dto instanceof SinapsiAlfaDataSourceDto sinapsiAlfaDataSourceDto) {
                sinapsiAlfaDataSource.generateMqttSettings(sinapsiAlfaConfig, sinapsiAlfaDataSourceDto.activationKey());
            } else {
                mqttDataSource.generateMqttSettings(mqttConfiguration, bCryptPasswordEncoder, plaintextPassword);
            }

            repository.save(dataSource);  // This save now perists the subscribe topic with the generated ID
        }

        startDataSource(dataSource);

        return new DataSourceSecretsDto(dataSource.id(), plaintextPassword);
    }

    public void deleteDataSource(UUID dataSourceId) {
        findDataSourceAdapter(dataSourceId).ifPresentOrElse(
                this::closeDataSourceAdapter,
                () -> LOGGER.warn(
                        "Data Source Adapter for data source ID {} not found.",
                        dataSourceId));

        repository.deleteById(dataSourceId);
    }

    public DataSource updateDataSource(DataSourceDto dto) throws InvalidUserException, EntityNotFoundException, ModbusConnectionException {
        var currentDataSource = repository.findById(dto.id())
                                          .orElseThrow(() -> new EntityNotFoundException(
                                                  "Datasource not found with ID: " + dto.id()));

        var currentEnabledState = currentDataSource.enabled();

        var currentUserId = authService.getCurrentUserId();
        var dataSource = DataSource.createFromDto(dto, currentUserId);

        findDataSourceAdapter(currentDataSource.id()).ifPresentOrElse(
                adapter -> updateDataSourceAdapterState(
                        adapter,
                        dataSource,
                        currentEnabledState,
                        dto.enabled()),
                () -> startDataSource(dataSource));


        var savedDataSource = repository.save(dataSource);
        LOGGER.debug("Updated data source {} with content {}", dto.id(), dto);

        return savedDataSource;
    }

    public void updateEnabledState(UUID dataSourceId, boolean enabled) throws EntityNotFoundException {
        DataSource dataSource = repository.findById(dataSourceId)
                                          .orElseThrow(() -> new EntityNotFoundException(
                                                  "Datasource not found with ID: " + dataSourceId));

        var currentEnabledState = dataSource.enabled();
        dataSource.setEnabled(enabled);

        findDataSourceAdapter(dataSourceId).ifPresentOrElse(
                adapter -> updateDataSourceAdapterState(
                        adapter,
                        dataSource,
                        currentEnabledState,
                        enabled),
                () -> startDataSource(dataSource));

        repository.save(dataSource);
        LOGGER.debug("Updated enabled state of data source {} to {}", dataSourceId, enabled);
    }

    public DataSourceSecretsDto regenerateSecrets(UUID dataSourceId) throws EntityNotFoundException {
        DataSource dataSource = repository.findById(dataSourceId)
                                          .orElseThrow(() -> new EntityNotFoundException(
                                                  "Datasource not found with ID: " + dataSourceId));

        var dataSourceSecrets = new DataSourceSecretsDto(dataSourceId, null);

        if (dataSource instanceof MqttDataSource mqttDataSource) {
            dataSourceSecrets = new DataSourceSecretsDto(dataSourceId, SecretGenerator.generate());
            mqttDataSource.setMqttPassword(bCryptPasswordEncoder.encode(dataSourceSecrets.plaintextPassword()));

            findDataSourceAdapter(dataSourceId).ifPresentOrElse(
                    adapter -> updateDataSourceAdapterState(
                            adapter,
                            dataSource,
                            dataSource.enabled(),
                            dataSource.enabled()),
                    () -> startDataSource(dataSource));

            repository.save(dataSource);
            LOGGER.debug("Regenerated secrets for data source {}", dataSourceId);
        }
        return dataSourceSecrets;
    }

    public Optional<DataSourceAdapter<? extends DataSource>> findDataSourceAdapter(UUID dataSourceId) {
        return findDataSourceAdapter(adapter -> adapter.dataSource().id().equals(dataSourceId));
    }

    public Optional<DataSourceAdapter<? extends DataSource>> findDataSourceAdapter(Predicate<DataSourceAdapter<? extends DataSource>> predicate) {
        return dataSourceAdapters.stream().filter(predicate).findFirst();
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
