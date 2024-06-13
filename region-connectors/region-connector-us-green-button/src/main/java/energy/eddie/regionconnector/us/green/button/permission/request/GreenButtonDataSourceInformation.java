package energy.eddie.regionconnector.us.green.button.permission.request;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.us.green.button.GreenButtonRegionConnectorMetadata;
import jakarta.persistence.Embeddable;

@Embeddable
@SuppressWarnings("NullAway")
public class GreenButtonDataSourceInformation implements DataSourceInformation {
    private static final GreenButtonRegionConnectorMetadata regionConnectorMetadata = GreenButtonRegionConnectorMetadata.getInstance();
    private final String dsoId;
    private final String countryCode;

    public GreenButtonDataSourceInformation(String dsoId, String countryCode) {
        this.dsoId = dsoId;
        this.countryCode = countryCode;
    }

    protected GreenButtonDataSourceInformation() {
        this.dsoId = null;
        this.countryCode = null;
    }

    @Override
    public String countryCode() {
        return countryCode;
    }

    @Override
    public String regionConnectorId() {
        return regionConnectorMetadata.id();
    }

    @Override
    public String meteredDataAdministratorId() {
        return dsoId;
    }

    @Override
    public String permissionAdministratorId() {
        return dsoId;
    }
}
