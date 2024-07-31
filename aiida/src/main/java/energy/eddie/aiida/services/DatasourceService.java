package energy.eddie.aiida.services;

import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.datasources.at.configs.OesterreichsEnergieAdapterConfiguration;
import energy.eddie.aiida.datasources.simulation.configs.SimulationDataSourceConfiguration;
import org.springframework.stereotype.Service;

@Service
public class DatasourceService {
    private final Aggregator aggregator;
    private final OesterreichsEnergieAdapterConfiguration oeaConfiguration;
    private final SimulationDataSourceConfiguration simulationDataSourceConfiguration;

    public DatasourceService(
            Aggregator aggregator,
            OesterreichsEnergieAdapterConfiguration oeaConfiguration,
            SimulationDataSourceConfiguration simulationDataSourceConfiguration
    ) {
        this.aggregator = aggregator;
        this.oeaConfiguration = oeaConfiguration;
        this.simulationDataSourceConfiguration = simulationDataSourceConfiguration;

        addSimulationDataSourcesToAggregator();
        addOesterreichsEnergieAdaptersToAggregator();
    }

    private void addSimulationDataSourcesToAggregator() {
        for (var simulationDataSource : simulationDataSourceConfiguration.enabledDataSources()) {
            aggregator.addNewAiidaDataSource(simulationDataSource);
        }
    }

    private void addOesterreichsEnergieAdaptersToAggregator() {
        for (var oea : oeaConfiguration.enabledDataSources()) {
            aggregator.addNewAiidaDataSource(oea);
        }
    }
}
