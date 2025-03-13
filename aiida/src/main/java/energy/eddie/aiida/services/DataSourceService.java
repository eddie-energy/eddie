package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.errors.InvalidUserException;
import energy.eddie.aiida.models.datasource.*;
import energy.eddie.aiida.repositories.DataSourceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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

        startDataSources();
    }

    public Optional<DataSource> getDataSourceById(UUID dataSourceId) {
        return repository.findById(dataSourceId);
    }

    public List<DataSource> getDataSources() throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();

        return repository.findByUserId(currentUserId);
    }

    public void addDataSource(DataSourceDto dto) throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();

        var mqttSettingsDto = new DataSourceMqttDto(
                mqttConfiguration.host(),
                "aiida/" + currentUserId + "/" + UUID.randomUUID(),
                MqttSecretGenerator.generate(),
                MqttSecretGenerator.generate()
        );
        var dataSource = DataSource.createFromDto(dto, currentUserId, mqttSettingsDto);

        repository.save(dataSource);
        startDataSource(dataSource);
    }

    public void deleteDataSource(UUID dataSourceId) {
        findDataSourceAdapter(dataSourceId).ifPresentOrElse(
                this::closeDataSource,
                () -> LOGGER.warn("Data Source Adapter for data source ID {} not found.", dataSourceId)
        );

        repository.deleteById(dataSourceId);
    }

    public DataSource updateDataSource(DataSourceDto dto) throws InvalidUserException, EntityNotFoundException {
        var currentDataSource = repository.findById(dto.id())
                                          .orElseThrow(() -> new EntityNotFoundException(
                                                  "Datasource not found with ID: " + dto.id()
                                          ));

        var currentUserId = authService.getCurrentUserId();
        var dataSource = DataSource.createFromDto(dto, currentUserId, currentDataSource);

        boolean wasEnabled = dataSource.enabled();

        findDataSourceAdapter(dataSource.id()).ifPresentOrElse(
                adapter -> updateDataSourceAdapterState(adapter, dataSource, wasEnabled),
                () -> startDataSource(dataSource)
        );

        return repository.save(dataSource);
    }

    public void updateEnabledState(UUID dataSourceId, boolean enabled) throws EntityNotFoundException {
        DataSource dataSource = repository.findById(dataSourceId)
                                          .orElseThrow(() -> new EntityNotFoundException(
                                                  "Datasource not found with ID: " + dataSourceId));

        findDataSourceAdapter(dataSourceId).ifPresentOrElse(
                adapter -> updateDataSourceAdapterState(adapter, dataSource, enabled),
                () -> startDataSource(dataSource)
        );

        dataSource.setEnabled(enabled);
        repository.save(dataSource);
    }

    public Optional<DataSourceAdapter<? extends DataSource>> findDataSourceAdapter(UUID dataSourceId) {
        return dataSourceAdapters.stream()
                                 .filter(adapter -> adapter.dataSource().id().equals(dataSourceId))
                                 .findFirst();
    }

    private void startDataSources() {
        var dataSources = repository.findAll();

        for (var dataSource : dataSources) {
            startDataSource(dataSource);
        }
    }

    private void startDataSource(DataSource dataSource) {
        var dataSourceAdapter = DataSourceAdapter.create(dataSource, objectMapper);
        dataSourceAdapters.add(dataSourceAdapter);

        if (dataSource.enabled()) {
            aggregator.addNewDataSourceAdapter(dataSourceAdapter);
        }
    }

    private void closeDataSource(DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        aggregator.removeDataSourceAdapter(dataSourceAdapter);
        dataSourceAdapters.remove(dataSourceAdapter);
    }

    private void updateDataSourceAdapterState(
            DataSourceAdapter<? extends DataSource> dataSourceAdapter,
            DataSource dataSource,
            boolean enabled
    ) {
        if (!enabled && dataSource.enabled()) {
            closeDataSource(dataSourceAdapter);
        } else if (enabled && !dataSource.enabled()) {
            dataSource.setEnabled(true);
            startDataSource(dataSource);
        } else if (enabled && dataSource.enabled()) {
            closeDataSource(dataSourceAdapter);
            startDataSource(dataSource);
        }
    }
}
