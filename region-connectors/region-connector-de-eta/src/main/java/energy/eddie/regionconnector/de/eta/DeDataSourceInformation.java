package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.DataSourceInformation;

/**
 * Data source information for the German (DE) ETA Plus region connector.
 * This class provides information about the permission administrator and
 * metered data administrator for Germany.
 */
public class DeDataSourceInformation implements DataSourceInformation {
    
    private static final String PERMISSION_ADMINISTRATOR_ID = "eta-plus";
    private static final String METERED_DATA_ADMINISTRATOR_ID = "eta-plus";

    @Override
    public String countryCode() {
        return EtaRegionConnectorMetadata.getInstance().countryCode();
    }

    @Override
    public String regionConnectorId() {
        return EtaRegionConnectorMetadata.REGION_CONNECTOR_ID;
    }

    @Override
    public String permissionAdministratorId() {
        return PERMISSION_ADMINISTRATOR_ID;
    }

    @Override
    public String meteredDataAdministratorId() {
        return METERED_DATA_ADMINISTRATOR_ID;
    }
}
