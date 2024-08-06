package energy.eddie.aiida.services;

import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.datasources.api.DataSourceConfiguration;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatasourceService {
    private final Aggregator aggregator;
    private final List<DataSourceConfiguration> dataSourceConfigurations;

    public DatasourceService(
            Aggregator aggregator,
            List<DataSourceConfiguration> dataSourceConfigurations
    ) {
        this.aggregator = aggregator;
        this.dataSourceConfigurations = dataSourceConfigurations;

        addDataSourcesToAggregator();
    }

    private void addDataSourcesToAggregator() {
        for (var dataSources : dataSourceConfigurations) {
            for (var dataSource : dataSources.enabledDataSources()) {
                aggregator.addNewAiidaDataSource(dataSource);
            }
        }
    }
}
