package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;

public class DatadisDataSourceInformation implements DataSourceInformation {
    private static final DatadisRegionConnectorMetadata regionConnectorMetadata = DatadisRegionConnectorMetadata.getInstance();

    @Override
    public String countryCode() {
        return regionConnectorMetadata.countryCode();
    }

    @Override
    public String regionConnectorId() {
        return regionConnectorMetadata.id();
    }

    @Override
    public String permissionAdministratorId() {
        return "Datadis";
    }

    @Override
    public String meteredDataAdministratorId() {
        return "Not available"; // mapping does currently not exist
    }
}