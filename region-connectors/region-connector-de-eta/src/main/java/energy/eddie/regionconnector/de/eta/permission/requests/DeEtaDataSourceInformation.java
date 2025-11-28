package energy.eddie.regionconnector.de.eta.permission.requests;

import energy.eddie.api.agnostic.DataSourceInformation;
import jakarta.persistence.Embeddable;

import static energy.eddie.regionconnector.de.eta.DeEtaRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Embeddable
public class DeEtaDataSourceInformation implements DataSourceInformation {
    private static final String COUNTRY_CODE = "DE";
    private static final String ADMINISTRATOR = "ETA+";

    @Override
    public String countryCode() {
        return COUNTRY_CODE;
    }

    @Override
    public String regionConnectorId() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String meteredDataAdministratorId() {
        return ADMINISTRATOR;
    }

    @Override
    public String permissionAdministratorId() {
        return ADMINISTRATOR;
    }
}
