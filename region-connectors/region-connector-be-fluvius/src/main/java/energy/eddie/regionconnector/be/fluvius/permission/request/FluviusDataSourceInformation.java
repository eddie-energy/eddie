package energy.eddie.regionconnector.be.fluvius.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.regionconnector.be.fluvius.FluviusRegionConnectorMetadata;

public class FluviusDataSourceInformation implements DataSourceInformation {

    public static final String FLUVIUS = "Fluvius";

    @Override
    public String countryCode() {
        return "BE";
    }

    @Override
    public String regionConnectorId() {
        return FluviusRegionConnectorMetadata.REGION_CONNECTOR_ID;
    }

    @Override
    public String meteredDataAdministratorId() {
        return FLUVIUS;
    }

    @Override
    public String permissionAdministratorId() {
        return FLUVIUS;
    }
}
