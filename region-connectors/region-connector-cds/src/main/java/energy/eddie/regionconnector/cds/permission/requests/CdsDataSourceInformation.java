package energy.eddie.regionconnector.cds.permission.requests;

import energy.eddie.api.agnostic.DataSourceInformation;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

import static energy.eddie.regionconnector.cds.CdsRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Embeddable
public class CdsDataSourceInformation implements DataSourceInformation {
    @Column(name = "cds_server_id")
    private final long cdsServerId;

    public CdsDataSourceInformation(long cdsServerId) {this.cdsServerId = cdsServerId;}

    @SuppressWarnings("NullAway")
    protected CdsDataSourceInformation() {
        cdsServerId = 0;
    }

    @Override
    public String countryCode() {
        return "";
    }

    @Override
    public String regionConnectorId() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String meteredDataAdministratorId() {
        return Objects.toString(cdsServerId);
    }

    @Override
    public String permissionAdministratorId() {
        return meteredDataAdministratorId();
    }

    public Long cdsServerId() {
        return cdsServerId;
    }
}
