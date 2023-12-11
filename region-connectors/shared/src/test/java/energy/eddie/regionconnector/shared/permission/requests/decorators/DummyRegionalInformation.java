package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.v0.RegionalInformation;

public class DummyRegionalInformation implements RegionalInformation {
    @Override
    public String countryCode() {
        return null;
    }

    @Override
    public String regionConnectorId() {
        return null;
    }

    @Override
    public String meteringDataAdministratorId() {
        return null;
    }

    @Override
    public String permissionAdministratorId() {
        return null;
    }
}