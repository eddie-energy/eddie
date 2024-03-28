package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

public record SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId,
                                      Optional<String> usagePointId,
                                      LocalDate startDate, LocalDate endDate,
                                      PermissionRequestState state,
                                      Granularity granularity) implements FrEnedisPermissionRequest {
    public SimplePermissionRequest(String permissionId, String connectionId) {
        this(permissionId, connectionId, null, Optional.empty(), null, null, null, Granularity.P1D);
    }

    @Override
    public FrEnedisPermissionRequest withStateBuilderFactory(StateBuilderFactory factory) {
        return this;
    }

    @Override
    public void setUsagePointId(String usagePointId) {

    }

    @Override
    public PermissionProcessStatus status() {
        return state.status();
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
    public LocalDate start() {
        return startDate;
    }

    @Override
    public LocalDate end() {
        return endDate;
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return Optional.empty();
    }

    @Override
    public void updateLatestMeterReadingEndDate(LocalDate latestMeterReading) {

    }
}
