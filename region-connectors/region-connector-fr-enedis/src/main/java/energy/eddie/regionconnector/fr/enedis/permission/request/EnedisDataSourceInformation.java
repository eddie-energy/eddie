package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata;

public class EnedisDataSourceInformation implements DataSourceInformation {
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
    public String meteredDataAdministratorId() {
        return ENEDIS;
    }

    @Override
    public String permissionAdministratorId() {
        return ENEDIS;
    }
}
