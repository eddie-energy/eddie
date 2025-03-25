package energy.eddie.aiida.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.adapters.datasource.at.OesterreichsEnergieAdapter;
import energy.eddie.aiida.adapters.datasource.fr.MicroTeleinfoV3Adapter;
import energy.eddie.aiida.adapters.datasource.sga.SmartGatewaysAdapter;
import energy.eddie.aiida.adapters.datasource.simulation.SimulationAdapter;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DataSourceAdapterTest {
    private static final UUID ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private ObjectMapper mapper;

    DataSource createNewDataSource(DataSourceType type) {
        return DataSource.createFromDto(
                new DataSourceDto(ID, type.identifier(), AiidaAsset.SUBMETER.asset(), "test", true, "", 1, null),
                ID,
                new DataSourceMqttDto("tcp://localhost:1883","tcp://localhost:1883", "aiida/test", "user", "pw")
        );
    }

    @BeforeEach
    void setUp() {
        mapper = new AiidaConfiguration().customObjectMapper().build();
    }

    @Test
    void givenOesterreichsEnergie_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.SMART_METER_ADAPTER);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper);

        // Then
        assertInstanceOf(OesterreichsEnergieAdapter.class, adapter);
    }

    @Test
    void givenMicroTeleinfo_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.MICRO_TELEINFO);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper);

        // Then
        assertInstanceOf(MicroTeleinfoV3Adapter.class, adapter);
    }

    @Test
    void givenSmartGateways_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.SMART_GATEWAYS_ADAPTER);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper);

        // Then
        assertInstanceOf(SmartGatewaysAdapter.class, adapter);
    }

    @Test
    void givenSimulation_returnsAdapter() {
        // Given
        var dataSource = createNewDataSource(DataSourceType.SIMULATION);

        // When
        var adapter = DataSourceAdapter.create(dataSource, mapper);

        // Then
        assertInstanceOf(SimulationAdapter.class, adapter);
    }
}
