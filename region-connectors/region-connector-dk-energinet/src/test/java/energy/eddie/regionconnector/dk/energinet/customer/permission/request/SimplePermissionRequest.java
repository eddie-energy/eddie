package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

public record SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId, ZonedDateTime start,
                                      ZonedDateTime end,
                                      PermissionRequestState state) implements DkEnerginetCustomerPermissionRequest {
    public SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId) {
        this(permissionId, connectionId, dataNeedId, null, null, null);
    }

    public SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId, ZonedDateTime start, ZonedDateTime end) {
        this(permissionId, connectionId, dataNeedId, start, end, null);
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
    public void changeState(PermissionRequestState state) {

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
}