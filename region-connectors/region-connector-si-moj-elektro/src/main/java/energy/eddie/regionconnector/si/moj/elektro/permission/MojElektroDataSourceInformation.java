package energy.eddie.regionconnector.si.moj.elektro.permission;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.regionconnector.si.moj.elektro.MojElektroRegionConnectorMetadata;

public class MojElektroDataSourceInformation implements DataSourceInformation {

    private static final String MOJ_ELEKTRO = "Moj Elektro";

    @Override
    public String countryCode() {
        return MojElektroRegionConnectorMetadata.COUNTRY_CODE;
    }

    @Override
    public String regionConnectorId() {
        return MojElektroRegionConnectorMetadata.REGION_CONNECTOR_ID;
    }

    @Override
    public String meteredDataAdministratorId() {
        return MOJ_ELEKTRO;
    }

    @Override
    public String permissionAdministratorId() {
        return MOJ_ELEKTRO;
    }
}
