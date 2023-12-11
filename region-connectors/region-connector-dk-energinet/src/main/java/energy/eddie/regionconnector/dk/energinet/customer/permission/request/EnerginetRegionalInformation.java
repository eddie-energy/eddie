package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.RegionalInformation;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;

public class EnerginetRegionalInformation implements RegionalInformation {
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
    public String meteringDataAdministratorId() {
        return ENERGINET;
    }
}