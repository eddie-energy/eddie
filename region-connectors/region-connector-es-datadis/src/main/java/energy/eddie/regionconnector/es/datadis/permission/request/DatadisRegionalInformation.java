package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.RegionalInformation;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;

public class DatadisRegionalInformation implements RegionalInformation {
    private static final DatadisRegionConnectorMetadata regionConnectorMetadata = DatadisRegionConnectorMetadata.getInstance();

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
        return "Datadis";
    }

    @Override
    public String meteringDataAdministratorId() {
        return "Not available"; // mapping does currently not exist
    }
}