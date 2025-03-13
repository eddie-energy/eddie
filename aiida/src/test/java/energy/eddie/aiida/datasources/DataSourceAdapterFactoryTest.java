package energy.eddie.aiida.datasources;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.datasources.at.OesterreichsEnergieAdapter;
import energy.eddie.aiida.datasources.fr.MicroTeleinfoV3Adapter;
import energy.eddie.aiida.datasources.sga.SmartGatewaysAdapter;
import energy.eddie.aiida.datasources.simulation.SimulationAdapter;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceFactory;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class DataSourceAdapterFactoryTest {
    private static final UUID ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private ObjectMapper mapper;

    DataSource createNewDataSource(DataSourceType type) {
        return DataSourceFactory.createFromDto(
                new DataSourceDto(ID, type.identifier(), AiidaAsset.SUBMETER.asset(), "test", true, "", 1, null),
                ID,
                new DataSourceMqttDto("tcp://localhost:1883", "aiida/test", "user", "pw")
        );
    }

    @BeforeEach
    void setUp() {
        mapper = new AiidaConfiguration().objectMapper();
    }

    @Test
    void givenOesterreichsEnergie_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.SMART_METER_ADAPTER);

        // When
        var adapter = DataSourceAdapterFactory.create(dataSource, mapper);

        // Then
        assertInstanceOf(OesterreichsEnergieAdapter.class, adapter);
    }

    @Test
    void givenMicroTeleinfo_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.MICRO_TELEINFO);

        // When
        var adapter = DataSourceAdapterFactory.create(dataSource, mapper);

        // Then
        assertInstanceOf(MicroTeleinfoV3Adapter.class, adapter);
    }

    @Test
    void givenSmartGateways_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.SMART_GATEWAYS_ADAPTER);

        // When
        var adapter = DataSourceAdapterFactory.create(dataSource, mapper);

        // Then
        assertInstanceOf(SmartGatewaysAdapter.class, adapter);
    }

    @Test
    void givenSimulation_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.SIMULATION);

        // When
        var adapter = DataSourceAdapterFactory.create(dataSource, mapper);

        // Then
        assertInstanceOf(SimulationAdapter.class, adapter);
    }
}
