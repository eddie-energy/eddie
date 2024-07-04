package energy.eddie.regionconnector.fi.fingrid.permission;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.fi.fingrid.FingridRegionConnectorMetadata;

public class FingridDataSourceInformation implements DataSourceInformation {

    private static final RegionConnectorMetadata REGION_CONNECTOR_METADATA = FingridRegionConnectorMetadata.INSTANCE;

    @Override
    public String countryCode() {
        return REGION_CONNECTOR_METADATA.countryCode();
    }

    @Override
    public String regionConnectorId() {
        return REGION_CONNECTOR_METADATA.id();
    }

    @Override
    public String meteredDataAdministratorId() {
        return "Fingrid";
    }

    @Override
    public String permissionAdministratorId() {
        return "Fingrid";
    }
}
