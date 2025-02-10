package energy.eddie.aiida.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.config.MQTTConfiguration;
import energy.eddie.aiida.datasources.AiidaDataSource;
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
    private MQTTConfiguration mqttConfiguration;
    private ObjectMapper objectMapper;
    private DataSourceService dataSourceService;
    private UUID userId;
    private DataSourceDto smartGatewaysDataSourceDto;
    private OesterreichsEnergieDataSource oesterreichsEnergieDataSource;
    private MicroTeleinfoV3DataSource microTeleinfoV3DataSource;
    private SimulationDataSource simulationDataSource;

    @BeforeEach
    void setUp() {
        repository = mock(DataSourceRepository.class);
        aggregator = mock(Aggregator.class);
        authService = mock(AuthService.class);
        mqttConfiguration = mock(MQTTConfiguration.class);
        objectMapper = mock(ObjectMapper.class);
        userId = UUID.randomUUID();
        dataSourceService = spy(new DataSourceService(repository, aggregator, authService, mqttConfiguration, objectMapper));
        smartGatewaysDataSourceDto = new DataSourceDto(
                1L,
                DataSourceType.SMART_GATEWAYS_ADAPTER.getIdentifier(),
                AiidaAsset.CONNECTION_AGREEMENT_POINT.getValue(),
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
        dataSource.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(dataSource));

        var result = dataSourceService.getDataSourceById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void shouldReturnEmptyOptionalWhenDataSourceNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        var result = dataSourceService.getDataSourceById(1L);

        assertTrue(result.isEmpty(), "Expected Optional to be empty when DataSource is not found.");
    }

    @Test
    void shouldAddNewDataSource() throws InvalidUserException {
        when(authService.getCurrentUserId()).thenReturn(userId);
        when(mqttConfiguration.host()).thenReturn("mqtt://test-broker");

        dataSourceService.addDataSource(smartGatewaysDataSourceDto);

        verify(repository, times(1)).save(any());
        verify(aggregator, times(1)).addNewAiidaDataSource(any());
    }

    @Test
    void shouldDeleteDataSource() {
        doNothing().when(aggregator).addNewAiidaDataSource(any());
        dataSourceService.deleteDataSource(1L);

        verify(repository, times(1)).deleteById(1L);
    }


    @Test
    void shouldUpdateDataSource() throws InvalidUserException {
        oesterreichsEnergieDataSource.setId(1000L);
        oesterreichsEnergieDataSource.setName("Old Name");
        oesterreichsEnergieDataSource.setMqttServerUri("old-uri");
        oesterreichsEnergieDataSource.setEnabled(false);

        when(repository.findById(1000L)).thenReturn(Optional.of(oesterreichsEnergieDataSource));
        when(repository.save(any(OesterreichsEnergieDataSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var updatedDataSource = new DataSourceDto(
                1000L,
                DataSourceType.SMART_METER_ADAPTER.getIdentifier(),
                AiidaAsset.CONNECTION_AGREEMENT_POINT.getValue(),
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
    void shouldLoadDataSourcesOnStartup() {
        oesterreichsEnergieDataSource.setId(1L);
        oesterreichsEnergieDataSource.setName("DataSource1");
        oesterreichsEnergieDataSource.setDataSourceType(DataSourceType.SMART_METER_ADAPTER);
        oesterreichsEnergieDataSource.setEnabled(true);

        microTeleinfoV3DataSource.setId(2L);
        microTeleinfoV3DataSource.setName("DataSource2");
        microTeleinfoV3DataSource.setDataSourceType(DataSourceType.MICRO_TELEINFO_V3);
        microTeleinfoV3DataSource.setEnabled(false);
        microTeleinfoV3DataSource.setMqttSubscribeTopic("mqttTopic");

        simulationDataSource.setId(3L);
        simulationDataSource.setName("DataSource3");
        simulationDataSource.setDataSourceType(DataSourceType.SIMULATION);
        simulationDataSource.setEnabled(true);
        simulationDataSource.setSimulationPeriod(5);

        when(repository.findAll()).thenReturn(List.of(oesterreichsEnergieDataSource, microTeleinfoV3DataSource, simulationDataSource));

        new DataSourceService(repository, aggregator, authService, mqttConfiguration, objectMapper);

        verify(aggregator, times(2)).addNewAiidaDataSource(any());
        verify(aggregator, never()).addNewAiidaDataSource(argThat(ds -> ds.id().equals("2")));
        verify(repository, times(2)).findAll();
    }

    @Test
    void testEnableDataSource() {
        microTeleinfoV3DataSource.setId(1L);
        microTeleinfoV3DataSource.setName("DataSource");
        microTeleinfoV3DataSource.setDataSourceType(DataSourceType.MICRO_TELEINFO_V3);
        microTeleinfoV3DataSource.setMqttSubscribeTopic("mqttTopic");
        microTeleinfoV3DataSource.setEnabled(false);
        var mockAiidaDataSource = mock(AiidaDataSource.class);

        when(repository.findById(1L)).thenReturn(Optional.of(microTeleinfoV3DataSource));
        doReturn(Optional.of(mockAiidaDataSource)).when(dataSourceService).findAiidaDataSource(1L);

        dataSourceService.updateEnabledState(1L, true);

        verify(repository, times(1)).save(argThat(DataSource::isEnabled));
        verify(aggregator, times(1)).addNewAiidaDataSource(any());

        dataSourceService.updateEnabledState(1L, false);

        verify(repository, times(2)).save(argThat(ds -> !ds.isEnabled()));
        verify(aggregator, times(1)).removeAiidaDataSource(any());
    }
}
