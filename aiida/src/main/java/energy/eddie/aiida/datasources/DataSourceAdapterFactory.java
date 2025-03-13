package energy.eddie.aiida.datasources;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.datasources.at.OesterreichsEnergieAdapter;
import energy.eddie.aiida.datasources.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.datasources.fr.MicroTeleinfoV3Adapter;
import energy.eddie.aiida.datasources.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.datasources.sga.SmartGatewaysAdapter;
import energy.eddie.aiida.datasources.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.datasources.simulation.SimulationAdapter;
import energy.eddie.aiida.datasources.simulation.SimulationDataSource;
import energy.eddie.aiida.models.datasource.DataSource;

public class DataSourceAdapterFactory {
    private DataSourceAdapterFactory() {}

    public static DataSourceAdapter<? extends DataSource> create(DataSource dataSource, ObjectMapper objectMapper) {
        return switch (dataSource.dataSourceType()) {
            case SMART_METER_ADAPTER ->
                    new OesterreichsEnergieAdapter((OesterreichsEnergieDataSource) dataSource, objectMapper);
            case MICRO_TELEINFO -> new MicroTeleinfoV3Adapter((MicroTeleinfoV3DataSource) dataSource, objectMapper);
            case SMART_GATEWAYS_ADAPTER -> new SmartGatewaysAdapter((SmartGatewaysDataSource) dataSource);
            case SIMULATION -> new SimulationAdapter((SimulationDataSource) dataSource);
        };
    }
}
