package energy.eddie.regionconnector.cds.permission.requests;

import energy.eddie.api.agnostic.DataSourceInformation;
import jakarta.persistence.Embeddable;

import static energy.eddie.regionconnector.cds.CdsRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Embeddable
public class CdsDataSourceInformation implements DataSourceInformation {
    private final String cdsServerName;

    public CdsDataSourceInformation(String cdsServerName) {this.cdsServerName = cdsServerName;}

    @SuppressWarnings("NullAway")
    protected CdsDataSourceInformation() {
        cdsServerName = null;
    }

    @Override
    public String countryCode() {
        return "";
    }

    @Override
    public String regionConnectorId() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String meteredDataAdministratorId() {
        return cdsServerName;
    }

    @Override
    public String permissionAdministratorId() {
        return cdsServerName;
    }
}
