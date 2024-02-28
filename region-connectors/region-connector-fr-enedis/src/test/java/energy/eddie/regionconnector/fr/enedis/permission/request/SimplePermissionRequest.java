package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

import java.time.ZonedDateTime;
import java.util.Optional;

public record SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId,
                                      Optional<String> usagePointId,
                                      ZonedDateTime start, ZonedDateTime end,
                                      PermissionRequestState state,
                                      Granularity granularity) implements FrEnedisPermissionRequest {
    public SimplePermissionRequest(String permissionId, String connectionId) {
        this(permissionId, connectionId, null, Optional.empty(), null, null, null, Granularity.P1D);
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return new EnedisDataSourceInformation();
    }

    @Override
    public ZonedDateTime created() {
        return null;
    }

    @Override
    public void changeState(PermissionRequestState state) {

    }

    @Override
    public void setUsagePointId(String usagePointId) {

    }
}