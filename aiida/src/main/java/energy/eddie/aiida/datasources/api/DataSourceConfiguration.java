package energy.eddie.aiida.datasources.api;

import energy.eddie.aiida.datasources.AiidaDataSource;

import java.util.Set;

public interface DataSourceConfiguration {
    Set<AiidaDataSource> enabledDataSources();
}
