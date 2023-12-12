package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.v0.DataSourceInformation;

public class DummyDataSourceInformation implements DataSourceInformation {
    @Override
    public String countryCode() {
        return null;
    }

    @Override
    public String regionConnectorId() {
        return null;
    }

    @Override
    public String meteredDataAdministratorId() {
        return null;
    }

    @Override
    public String permissionAdministratorId() {
        return null;
    }
}