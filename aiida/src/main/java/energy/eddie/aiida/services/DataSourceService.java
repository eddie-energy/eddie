package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceModbusDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.errors.InvalidUserException;
import energy.eddie.aiida.errors.ModbusConnectionException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.MqttSecretGenerator;
import energy.eddie.aiida.repositories.DataSourceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
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

    @Autowired
    public DataSourceService(
            DataSourceRepository repository,
            Aggregator aggregator,
            AuthService authService,
            MqttConfiguration mqttConfiguration,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.aggregator = aggregator;
        this.authService = authService;
        this.mqttConfiguration = mqttConfiguration;
        this.objectMapper = objectMapper;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void startDataSources() {
        var dataSources = repository.findAll();

        for (var dataSource : dataSources) {
            startDataSource(dataSource);
        }
    }

    public void startDataSource(DataSource dataSource) throws ModbusConnectionException {
        var dataSourceAdapter = DataSourceAdapter.create(dataSource, objectMapper);
        dataSourceAdapters.add(dataSourceAdapter);

        if (dataSource.enabled()) {
            aggregator.addNewDataSourceAdapter(dataSourceAdapter);
        }
    }

    public Optional<DataSource> dataSourceById(UUID dataSourceId) {
        return repository.findById(dataSourceId);
    }

    public List<DataSource> getDataSources() throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();

        return repository.findByUserId(currentUserId);
    }

    public void addDataSource(DataSourceDto dto) throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();

        if (dto.dataSourceType().equals(DataSourceType.MODBUS)) {
            var modbusSettings = dto.modbusSettings();
            if (modbusSettings != null) {
                var modbusSettingsDto = new DataSourceModbusDto(
                        modbusSettings.modbusIp(),
                        modbusSettings.modbusVendor(),
                        modbusSettings.modbusModel(),
                        modbusSettings.modbusDevice()
                );
                var dataSource = DataSource.createFromDto(dto, currentUserId, modbusSettingsDto);
                startDataSource(dataSource);
                repository.save(dataSource);
            }
        } else {
            var mqttSettingsDto = new DataSourceMqttDto(
                    mqttConfiguration.internalHost(),
                    mqttConfiguration.externalHost(),
                    "aiida/" + MqttSecretGenerator.generate(),
                    MqttSecretGenerator.generate(),
                    MqttSecretGenerator.generate()
            );
            var dataSource = DataSource.createFromDto(dto, currentUserId, mqttSettingsDto);

            // This save generates the datasource ID
            repository.save(dataSource);

            if (dataSource instanceof MqttDataSource) {
                // This save now perists the subscribe topic with the generated ID
                repository.save(dataSource);
            }
            startDataSource(dataSource);
        }
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
        var dataSource = currentDataSource.mergeWithDto(dto, currentUserId);

        findDataSourceAdapter(currentDataSource.id()).ifPresentOrElse(
                adapter -> updateDataSourceAdapterState(
                        adapter,
                        dataSource,
                        currentEnabledState,
                        dto.enabled()),
                () -> startDataSource(dataSource));

        return repository.save(dataSource);
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
    }

    public Optional<DataSourceAdapter<? extends DataSource>> findDataSourceAdapter(UUID dataSourceId) {
        return findDataSourceAdapter(adapter -> adapter.dataSource().id().equals(dataSourceId));
    }

    public Optional<DataSourceAdapter<? extends DataSource>> findDataSourceAdapter(Predicate<DataSourceAdapter<? extends DataSource>> predicate) {
        return dataSourceAdapters.stream().filter(predicate).findFirst();
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
