package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.config.MQTTConfiguration;
import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.datasources.at.OesterreichsEnergieAdapter;
import energy.eddie.aiida.datasources.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.datasources.fr.MicroTeleinfoV3;
import energy.eddie.aiida.datasources.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.datasources.sga.SmartGatewaysAdapter;
import energy.eddie.aiida.datasources.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.datasources.simulation.SimulationDataSource;
import energy.eddie.aiida.datasources.simulation.SimulationDataSourceAdapter;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.errors.InvalidUserException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.MQTTSecretGenerator;
import energy.eddie.aiida.models.datasource.MqttDataSource;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.utils.MqttConfig;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class DataSourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceService.class);

    private final DataSourceRepository repository;
    private final Aggregator aggregator;
    private final AuthService authService;
    private final Set<AiidaDataSource> aiidaDataSources = new HashSet<>();
    private final MQTTConfiguration mqttConfiguration;
    private final ObjectMapper objectMapper;

    @Autowired
    public DataSourceService(
            DataSourceRepository repository,
            Aggregator aggregator,
            AuthService authService,
            MQTTConfiguration mqttConfiguration,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.aggregator = aggregator;
        this.authService = authService;
        this.mqttConfiguration = mqttConfiguration;
        this.objectMapper = objectMapper;

        loadDataSources();
    }

    public Optional<DataSource> getDataSourceById(Long dataSourceId) {
        return repository.findById(dataSourceId);
    }

    private void loadDataSources() {
        var dataSources = repository.findAll();

        for (var dataSource : dataSources) {
            startDataSource(dataSource);
        }
    }

    public List<DataSource> getDataSources() throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();

        return repository.findByUserId(currentUserId);
    }

    public void addDataSource(DataSourceDto addDataSource) throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();
        var dataSourceType = DataSourceType.fromIdentifier(addDataSource.dataSourceType());
        final DataSource dataSource = createOrUpdate(addDataSource);

        if (dataSource instanceof MqttDataSource mqttDataSource) {
            final String mqttServerUri = mqttConfiguration.host();
            final String mqttUsername = addDataSource.name() + "-username-" + MQTTSecretGenerator.generate();
            final String mqttPassword = addDataSource.name() + "-password-" + MQTTSecretGenerator.generate();

            final String mqttSubscribeTopic = dataSourceType == DataSourceType.MICRO_TELEINFO_V3
                    ? addDataSource.mqttSubscribeTopic() + "/" + addDataSource.meteringId()
                    : "aiida/" + addDataSource.name() + "/" + MQTTSecretGenerator.generate();

            mqttDataSource.setMqttServerUri(mqttServerUri);
            mqttDataSource.setMqttUsername(mqttUsername);
            mqttDataSource.setMqttPassword(mqttPassword);
            mqttDataSource.setMqttSubscribeTopic(mqttSubscribeTopic);

            if (dataSourceType == DataSourceType.MICRO_TELEINFO_V3) {
                ((MicroTeleinfoV3DataSource) mqttDataSource).setMeteringId(addDataSource.meteringId());
            }
        }

        dataSource.setUserId(currentUserId);
        repository.save(dataSource);

        startDataSource(dataSource);
    }

    public void deleteDataSource(Long dataSourceId) {
        findAiidaDataSource(dataSourceId).ifPresentOrElse(
                this::closeDataSource,
                () -> LOGGER.warn("AiidaDataSource for data source ID {} not found in aiidaDataSources.", dataSourceId)
        );

        repository.deleteById(dataSourceId);
    }

    public DataSource updateDataSource(DataSourceDto updateDataSource) throws InvalidUserException {
        var dataSource = createOrUpdate(updateDataSource);
        var currentUserId = authService.getCurrentUserId();
        dataSource.setUserId(currentUserId);

        boolean wasEnabled = dataSource.isEnabled();

        findAiidaDataSource(dataSource.getId()).ifPresentOrElse(
                ds -> updateAiidaDataSourceState(ds, dataSource, wasEnabled),
                () -> startDataSource(dataSource)
        );

        return repository.save(dataSource);
    }

    private void closeDataSource(AiidaDataSource aiidaDataSource) {
        aggregator.removeAiidaDataSource(aiidaDataSource);
        aiidaDataSources.remove(aiidaDataSource);
    }

    private void startDataSource(DataSource dataSource) {
        var newAiidaDataSource = transformDataSource(dataSource);
        aiidaDataSources.add(newAiidaDataSource);

        if (dataSource.isEnabled()) {
            aggregator.addNewAiidaDataSource(newAiidaDataSource);
        }
    }

    private AiidaDataSource transformDataSource(DataSource dataSource) {
        var dataSourceType = dataSource.getDataSourceType();

        if (dataSourceType == DataSourceType.SIMULATION) {
            var simDataSource = (SimulationDataSource) dataSource;
            var simulationPeriod = simDataSource.getSimulationPeriod() == null ? 5 : simDataSource.getSimulationPeriod();
            return new SimulationDataSourceAdapter(String.valueOf(dataSource.getId()), Duration.ofSeconds(simulationPeriod));
        }

        var mqttDataSource = (MqttDataSource) dataSource;
        var mqttBuildConfig = new MqttConfig.MqttConfigBuilder(
                mqttDataSource.getMqttServerUri(), mqttDataSource.getMqttSubscribeTopic());
        mqttBuildConfig.setUsername(mqttDataSource.getMqttUsername());
        mqttBuildConfig.setPassword(mqttDataSource.getMqttPassword());

        var mqttConfig = new MqttConfig(mqttBuildConfig);

        return switch (dataSourceType) {
            case SMART_GATEWAYS_ADAPTER ->
                    new SmartGatewaysAdapter(String.valueOf(dataSource.getId()), mqttConfig);
            case MICRO_TELEINFO_V3 ->
                    new MicroTeleinfoV3(String.valueOf(dataSource.getId()), mqttConfig, objectMapper);
            case SMART_METER_ADAPTER ->
                    new OesterreichsEnergieAdapter(String.valueOf(dataSource.getId()), mqttConfig, objectMapper);
            default -> {
                LOGGER.error("Unknown data source type: {}", dataSourceType);
                throw new IllegalArgumentException("Unknown data source type: " + dataSourceType);
            }
        };
    }

    public void updateEnabledState(Long dataSourceId, boolean enabled) {
        DataSource dataSource = repository.findById(dataSourceId)
                                          .orElseThrow(() -> new EntityNotFoundException("Datasource not found with ID: " + dataSourceId));

        findAiidaDataSource(dataSourceId).ifPresentOrElse(
                ds -> updateAiidaDataSourceState(ds, dataSource, enabled),
                () -> startDataSource(dataSource)
        );

        dataSource.setEnabled(enabled);
        repository.save(dataSource);
    }

    public DataSource createOrUpdate(DataSourceDto dataSourceDto) throws InvalidUserException {
        DataSource dataSource = (dataSourceDto.id() == null)
                ? createNewDataSource(dataSourceDto)
                : repository.findById(dataSourceDto.id()).orElse(null);

        if (dataSource == null) {
            dataSource = createNewDataSource(dataSourceDto);
        }

        final DataSourceType dataSourceType = DataSourceType.fromIdentifier(dataSourceDto.dataSourceType());
        dataSource.setName(dataSourceDto.name());
        dataSource.setEnabled(dataSourceDto.enabled());
        dataSource.setAsset(AiidaAsset.forValue(dataSourceDto.asset()));
        dataSource.setDataSourceType(dataSourceType);

        switch (dataSource) {
            case SimulationDataSource simDataSource -> simDataSource.setSimulationPeriod(dataSourceDto.simulationPeriod());
            case MqttDataSource mqttDataSource -> {
                mqttDataSource.setMqttServerUri(dataSourceDto.mqttServerUri());
                mqttDataSource.setMqttUsername(dataSourceDto.mqttUsername());
                mqttDataSource.setMqttPassword(dataSourceDto.mqttPassword());
                mqttDataSource.setMqttSubscribeTopic(dataSourceDto.mqttSubscribeTopic());

                if (mqttDataSource instanceof MicroTeleinfoV3DataSource microTeleinfoDataSource) {
                    microTeleinfoDataSource.setMeteringId(dataSourceDto.meteringId());
                }
            }
            default -> LOGGER.error("Failed to create data source: {}", dataSourceType);
        }

        return dataSource;
    }

    private DataSource createNewDataSource(DataSourceDto dataSourceDto) throws InvalidUserException {
        final DataSourceType dataSourceType = DataSourceType.fromIdentifier(dataSourceDto.dataSourceType());

        if (dataSourceType == DataSourceType.SIMULATION) {
            return new SimulationDataSource(
                    dataSourceDto.name(),
                    dataSourceDto.enabled(),
                    authService.getCurrentUserId(),
                    AiidaAsset.forValue(dataSourceDto.asset()),
                    dataSourceType,
                    dataSourceDto.simulationPeriod()
            );
        } else {
            final MqttDataSource mqttDataSource = switch (dataSourceType) {
                case SMART_GATEWAYS_ADAPTER -> new SmartGatewaysDataSource();
                case SMART_METER_ADAPTER -> new OesterreichsEnergieDataSource();
                case MICRO_TELEINFO_V3 -> new MicroTeleinfoV3DataSource();
                default -> throw new IllegalArgumentException("Unknown data source type: " + dataSourceType);
            };

            mqttDataSource.setMqttServerUri(dataSourceDto.mqttServerUri());
            mqttDataSource.setMqttUsername(dataSourceDto.mqttUsername());
            mqttDataSource.setMqttPassword(dataSourceDto.mqttPassword());
            mqttDataSource.setMqttSubscribeTopic(dataSourceDto.mqttSubscribeTopic());

            if (mqttDataSource instanceof MicroTeleinfoV3DataSource microTeleinfoDataSource) {
                microTeleinfoDataSource.setMeteringId(dataSourceDto.meteringId());
            }

            return mqttDataSource;
        }
    }

    Optional<AiidaDataSource> findAiidaDataSource(Long dataSourceId) {
        return aiidaDataSources.stream()
                               .filter(ds -> ds.id().equals(String.valueOf(dataSourceId)))
                               .findFirst();
    }

    private void updateAiidaDataSourceState(AiidaDataSource aiidaDataSource, DataSource dataSource, boolean enabled) {
        if (!enabled && dataSource.isEnabled()) {
            closeDataSource(aiidaDataSource);
        } else if (enabled && !dataSource.isEnabled()) {
            dataSource.setEnabled(true);
            startDataSource(dataSource);
        } else if (enabled && dataSource.isEnabled()) {
            closeDataSource(aiidaDataSource);
            startDataSource(dataSource);
        }
    }
}
