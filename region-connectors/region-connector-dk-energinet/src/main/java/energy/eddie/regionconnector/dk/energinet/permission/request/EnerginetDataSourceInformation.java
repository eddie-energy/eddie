package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;

public class EnerginetDataSourceInformation implements DataSourceInformation {
    private static final EnerginetRegionConnectorMetadata regionConnectorMetadata = EnerginetRegionConnectorMetadata.getInstance();

    private static final String ENERGINET = "Energinet";

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
        return ENERGINET;
    }

    @Override
    public String meteredDataAdministratorId() {
        return ENERGINET;
    }
}