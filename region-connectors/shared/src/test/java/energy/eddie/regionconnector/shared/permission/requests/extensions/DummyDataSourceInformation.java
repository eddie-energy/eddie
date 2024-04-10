package energy.eddie.regionconnector.shared.permission.requests.extensions;

import energy.eddie.api.v0.DataSourceInformation;

public class DummyDataSourceInformation implements DataSourceInformation {
    @Override
    public String countryCode() {
        return "AT";
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