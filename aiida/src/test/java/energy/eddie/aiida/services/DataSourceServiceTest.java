package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.datasources.DataSourceAdapter;
import energy.eddie.aiida.datasources.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.datasources.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.datasources.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.datasources.simulation.SimulationDataSource;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.errors.InvalidUserException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class DataSourceServiceTest {

    private DataSourceRepository repository;
    private Aggregator aggregator;
    private AuthService authService;
    private MqttConfiguration mqttConfiguration;
    private ObjectMapper objectMapper;
    private DataSourceService dataSourceService;
    private UUID userId;
    private DataSourceDto smartGatewaysDataSourceDto;
    private OesterreichsEnergieDataSource oesterreichsEnergieDataSource;
    private MicroTeleinfoV3DataSource microTeleinfoV3DataSource;
    private SimulationDataSource simulationDataSource;
    private static final UUID dataSourceId = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");

    @BeforeEach
    void setUp() {
        repository = mock(DataSourceRepository.class);
        aggregator = mock(Aggregator.class);
        authService = mock(AuthService.class);
        mqttConfiguration = mock(MqttConfiguration.class);
        objectMapper = mock(ObjectMapper.class);
        userId = UUID.randomUUID();
        dataSourceService = spy(new DataSourceService(repository, aggregator, authService, mqttConfiguration, objectMapper));
        smartGatewaysDataSourceDto = new DataSourceDto(
                dataSourceId,
                DataSourceType.SMART_GATEWAYS_ADAPTER.identifier(),
                AiidaAsset.CONNECTION_AGREEMENT_POINT.asset(),
                "Smart Gateways",
                true,
                "mqtt://test-broker",
                "mqttTopic",
                "mqttUsername",
                "mqttPassword",
                "", 0
        );
        oesterreichsEnergieDataSource = new OesterreichsEnergieDataSource();
        microTeleinfoV3DataSource = new MicroTeleinfoV3DataSource();
        simulationDataSource = new SimulationDataSource();
    }

    @Test
    void shouldReturnDataSourceById() {
        var dataSource = new SmartGatewaysDataSource();
        dataSource.setId(dataSourceId);
        when(repository.findById(dataSourceId)).thenReturn(Optional.of(dataSource));

        var result = dataSourceService.getDataSourceById(dataSourceId);

        assertTrue(result.isPresent());
        assertEquals(dataSourceId, result.get().getId());
        verify(repository, times(1)).findById(dataSourceId);
    }

    @Test
    void shouldReturnEmptyOptionalWhenDataSourceNotFound() {
        when(repository.findById(dataSourceId)).thenReturn(Optional.empty());

        var result = dataSourceService.getDataSourceById(dataSourceId);

        assertTrue(result.isEmpty(), "Expected Optional to be empty when DataSource is not found.");
    }

    @Test
    void shouldAddNewDataSource() throws InvalidUserException {
        when(authService.getCurrentUserId()).thenReturn(userId);
        when(mqttConfiguration.host()).thenReturn("mqtt://test-broker");

        dataSourceService.addDataSource(smartGatewaysDataSourceDto);

        verify(repository, times(1)).save(any());
        verify(aggregator, times(1)).addNewDataSourceAdapter(any());
    }

    @Test
    void shouldDeleteDataSource() {
        doNothing().when(aggregator).addNewDataSourceAdapter(any());
        dataSourceService.deleteDataSource(dataSourceId);

        verify(repository, times(1)).deleteById(dataSourceId);
    }


    @Test
    void shouldUpdateDataSource() throws InvalidUserException {
        oesterreichsEnergieDataSource.setId(dataSourceId);
        oesterreichsEnergieDataSource.setName("Old Name");
        oesterreichsEnergieDataSource.setMqttServerUri("old-uri");
        oesterreichsEnergieDataSource.setEnabled(false);

        when(repository.findById(dataSourceId)).thenReturn(Optional.of(oesterreichsEnergieDataSource));
        when(repository.save(any(OesterreichsEnergieDataSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var updatedDataSource = new DataSourceDto(
                dataSourceId,
                DataSourceType.SMART_METER_ADAPTER.identifier(),
                AiidaAsset.CONNECTION_AGREEMENT_POINT.asset(),
                "New Name",
                true,
                "new-uri",
                "mqttTopic",
                "mqttUsername",
                "mqttPassword",
                "", 0
        );

        var savedDataSource = (OesterreichsEnergieDataSource) dataSourceService.updateDataSource(updatedDataSource);

        assertEquals("New Name", savedDataSource.getName());
        assertEquals("new-uri", savedDataSource.getMqttServerUri());
        assertTrue(savedDataSource.isEnabled());
    }


    @Test
    void shouldStartDataSourcesOnStartup() {
        UUID dataSourceId1 = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
        UUID dataSourceId2 = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
        UUID dataSourceId3 = UUID.fromString("6211ea05-d4ab-48ff-8613-8f4791a56606");

        oesterreichsEnergieDataSource.setId(dataSourceId1);
        oesterreichsEnergieDataSource.setName("DataSource1");
        oesterreichsEnergieDataSource.setDataSourceType(DataSourceType.SMART_METER_ADAPTER);
        oesterreichsEnergieDataSource.setEnabled(true);

        microTeleinfoV3DataSource.setId(dataSourceId2);
        microTeleinfoV3DataSource.setName("DataSource2");
        microTeleinfoV3DataSource.setDataSourceType(DataSourceType.MICRO_TELEINFO_V3);
        microTeleinfoV3DataSource.setEnabled(false);
        microTeleinfoV3DataSource.setMqttSubscribeTopic("mqttTopic");

        simulationDataSource.setId(dataSourceId3);
        simulationDataSource.setName("DataSource3");
        simulationDataSource.setDataSourceType(DataSourceType.SIMULATION);
        simulationDataSource.setEnabled(true);
        simulationDataSource.setSimulationPeriod(5);

        when(repository.findAll()).thenReturn(List.of(oesterreichsEnergieDataSource, microTeleinfoV3DataSource, simulationDataSource));

        new DataSourceService(repository, aggregator, authService, mqttConfiguration, objectMapper);

        verify(aggregator, times(2)).addNewDataSourceAdapter(any());
        verify(aggregator, never()).addNewDataSourceAdapter(argThat(ds -> ds.id().equals(dataSourceId2)));
        verify(repository, times(2)).findAll();
    }

    @Test
    void testEnableDataSource() {
        microTeleinfoV3DataSource.setId(dataSourceId);
        microTeleinfoV3DataSource.setName("DataSource");
        microTeleinfoV3DataSource.setDataSourceType(DataSourceType.MICRO_TELEINFO_V3);
        microTeleinfoV3DataSource.setMqttSubscribeTopic("mqttTopic");
        microTeleinfoV3DataSource.setEnabled(false);
        var mockAiidaDataSource = mock(DataSourceAdapter.class);

        when(repository.findById(dataSourceId)).thenReturn(Optional.of(microTeleinfoV3DataSource));
        doReturn(Optional.of(mockAiidaDataSource)).when(dataSourceService).findAiidaDataSource(dataSourceId);

        dataSourceService.updateEnabledState(dataSourceId, true);

        verify(repository, times(1)).save(argThat(DataSource::isEnabled));
        verify(aggregator, times(1)).addNewDataSourceAdapter(any());

        dataSourceService.updateEnabledState(dataSourceId, false);

        verify(repository, times(2)).save(argThat(ds -> !ds.isEnabled()));
        verify(aggregator, times(1)).removeAiidaDataSource(any());
    }
}
