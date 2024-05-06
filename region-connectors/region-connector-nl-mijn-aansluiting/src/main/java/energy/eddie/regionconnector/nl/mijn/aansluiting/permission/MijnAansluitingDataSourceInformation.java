package energy.eddie.regionconnector.nl.mijn.aansluiting.permission;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata;
import jakarta.persistence.Embeddable;

@Embeddable
public class MijnAansluitingDataSourceInformation implements DataSourceInformation {
    private static final MijnAansluitingRegionConnectorMetadata regionConnectorMetadata = MijnAansluitingRegionConnectorMetadata.getInstance();

    private static final String MIIJN_AANSLUITING = "Miijn Aansluiting";

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
        return MIIJN_AANSLUITING;
    }

    @Override
    public String permissionAdministratorId() {
        return MIIJN_AANSLUITING;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MijnAansluitingDataSourceInformation;
    }
}