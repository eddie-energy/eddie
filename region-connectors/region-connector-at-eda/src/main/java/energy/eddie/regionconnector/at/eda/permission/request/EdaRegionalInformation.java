package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.RegionalInformation;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;

import java.util.Objects;

public class EdaRegionalInformation implements RegionalInformation {
    private static final EdaRegionConnectorMetadata regionConnectorMetadata = EdaRegionConnectorMetadata.getInstance();
    private final String dsoId;

    public EdaRegionalInformation(String dsoId) {
        this.dsoId = dsoId;
    }

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
        return dsoId;
    }

    @Override
    public String meteringDataAdministratorId() {
        return dsoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EdaRegionalInformation that)) return false;
        return Objects.equals(dsoId, that.dsoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dsoId);
    }
}