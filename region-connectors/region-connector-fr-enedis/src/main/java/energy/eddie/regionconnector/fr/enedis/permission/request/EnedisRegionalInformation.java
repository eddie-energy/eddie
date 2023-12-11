package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.v0.RegionalInformation;
import energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata;

public class EnedisRegionalInformation implements RegionalInformation {
    private static final EnedisRegionConnectorMetadata regionConnectorMetadata = EnedisRegionConnectorMetadata.getInstance();

    private static final String ENEDIS = "Enedis";

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
        return ENEDIS;
    }

    @Override
    public String meteringDataAdministratorId() {
        return ENEDIS;
    }
}