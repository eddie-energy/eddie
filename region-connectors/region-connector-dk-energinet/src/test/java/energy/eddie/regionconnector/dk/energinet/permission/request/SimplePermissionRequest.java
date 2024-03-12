package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Objects;

public final class SimplePermissionRequest implements DkEnerginetCustomerPermissionRequest {
    private final String permissionId;
    private final String connectionId;
    private final String dataNeedId;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final PermissionRequestState state;
    private ZonedDateTime lastPolled;

    public SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId, ZonedDateTime start,
                                   ZonedDateTime end, ZonedDateTime lastPolled,
                                   PermissionRequestState state) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.start = start;
        this.end = end;
        this.lastPolled = lastPolled;
        this.state = state;
    }

    public SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId, ZonedDateTime start,
                                   ZonedDateTime end,
                                   PermissionRequestState state) {
        this(permissionId, connectionId, dataNeedId, start, end, start, state);
    }

    public SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId) {
        this(permissionId, connectionId, dataNeedId, null, null, null);
    }

    public SimplePermissionRequest() {
        this(null, null, null, null, null, null);
    }

    public SimplePermissionRequest(ZonedDateTime start, ZonedDateTime end, ZonedDateTime lastPolled) {
        this(null, null, null, start, end, lastPolled, null);
    }

    @Override
    public String connectionId() {
        return connectionId;
    }

    @Override
    public String dataNeedId() {
        return dataNeedId;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return new EnerginetDataSourceInformation();
    }

    @Override
    public ZonedDateTime created() {
        return null;
    }

    @Override
    public void changeState(PermissionRequestState state) {

    }

    @Override
    public DkEnerginetCustomerPermissionRequest withApiClient(EnerginetCustomerApi client) {
        return null;
    }

    @Override
    public DkEnerginetCustomerPermissionRequest withStateBuilderFactory(StateBuilderFactory factory) {
        return null;
    }

    @Override
    public Mono<String> accessToken() {
        return Mono.empty();
    }

    @Override
    public Granularity granularity() {
        return Granularity.PT1H;
    }

    @Override
    public String meteringPoint() {
        return "meteringPoint";
    }

    @Override
    public ZonedDateTime lastPolled() {
        return lastPolled;
    }

    @Override
    public PermissionProcessStatus status() {
        return state.status();
    }

    @Override
    public void updateLastPolled(ZonedDateTime lastPolled) {
        this.lastPolled = lastPolled;
    }

    @Override
    public String permissionId() {
        return permissionId;
    }

    @Override
    public ZonedDateTime start() {
        return start;
    }

    @Override
    public ZonedDateTime end() {
        return end;
    }

    @Override
    public PermissionRequestState state() {
        return state;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SimplePermissionRequest) obj;
        return Objects.equals(this.permissionId, that.permissionId) &&
                Objects.equals(this.connectionId, that.connectionId) &&
                Objects.equals(this.dataNeedId, that.dataNeedId) &&
                Objects.equals(this.start, that.start) &&
                Objects.equals(this.end, that.end) &&
                Objects.equals(this.lastPolled, that.lastPolled) &&
                Objects.equals(this.state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionId, connectionId, dataNeedId, start, end, lastPolled, state);
    }

    @Override
    public String toString() {
        return "SimplePermissionRequest[" +
                "permissionId=" + permissionId + ", " +
                "connectionId=" + connectionId + ", " +
                "dataNeedId=" + dataNeedId + ", " +
                "start=" + start + ", " +
                "end=" + end + ", " +
                "lastPolled=" + lastPolled + ", " +
                "state=" + state + ']';
    }
}
