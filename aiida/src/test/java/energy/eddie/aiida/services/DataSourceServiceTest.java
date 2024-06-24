package energy.eddie.aiida.services;

import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.datasources.at.OesterreichsEnergieAdapter;
import energy.eddie.aiida.datasources.at.configs.OesterreichsEnergieAdapterConfiguration;
import energy.eddie.aiida.datasources.simulation.SimulationDataSource;
import energy.eddie.aiida.datasources.simulation.configs.SimulationDataSourceConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.Mockito.*;

class DataSourceServiceTest {
    @Test
    void testDataSourceService() {
        // given
        var aggregator = mock(Aggregator.class);
        var oeaConfiguration = mock(OesterreichsEnergieAdapterConfiguration.class);
        var simDataSourceConfiguration = mock(SimulationDataSourceConfiguration.class);
        var oea = mock(OesterreichsEnergieAdapter.class);
        var simDataSource = mock(SimulationDataSource.class);
        when(simDataSourceConfiguration.enabledDataSources()).thenReturn(Set.of(simDataSource));
        when(oeaConfiguration.enabledDataSources()).thenReturn(Set.of(oea));

        // when
        new DatasourceService(aggregator, oeaConfiguration, simDataSourceConfiguration);

        // then
        verify(aggregator, times(1)).addNewAiidaDataSource(simDataSource);
        verify(aggregator, times(1)).addNewAiidaDataSource(oea);
    }
}
