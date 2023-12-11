package energy.eddie.regionconnector.aiida.api;

import energy.eddie.api.v0.RegionalInformation;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;

public class AiidaRegionalInformation implements RegionalInformation {
    private static final AiidaRegionConnectorMetadata regionConnectorMetadata = AiidaRegionConnectorMetadata.getInstance();

    @Override
    public String countryCode() {
        return regionConnectorMetadata.countryCode();
    }

    @Override
    public String regionConnectorId() {
        return regionConnectorMetadata.id();
    }

    @Override
    public String meteringDataAdministratorId() {
        return regionConnectorMetadata.id();
    }

    @Override
    public String permissionAdministratorId() {
        return regionConnectorMetadata.id();
    }
}
