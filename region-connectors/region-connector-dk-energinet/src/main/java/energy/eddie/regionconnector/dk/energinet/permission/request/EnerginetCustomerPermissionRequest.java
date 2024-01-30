package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.states.EnerginetCustomerCreatedState;
import energy.eddie.regionconnector.shared.permission.requests.TimestampedPermissionRequest;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.DK_ZONE_ID;
import static java.util.Objects.requireNonNull;

public class EnerginetCustomerPermissionRequest extends TimestampedPermissionRequest implements DkEnerginetCustomerPermissionRequest {
    private static final EnerginetDataSourceInformation dataSourceInformation = new EnerginetDataSourceInformation();
    private final String permissionId;
    private final String connectionId;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final ApiCredentials credentials;
    private final String meteringPoint;
    private final String dataNeedId;
    private final Granularity granularity;
    private PermissionRequestState state;

    public EnerginetCustomerPermissionRequest(
            String permissionId,
            PermissionRequestForCreation request,
            EnerginetCustomerApi apiClient) {
        super(DK_ZONE_ID);
        requireNonNull(permissionId);
        requireNonNull(request);
        requireNonNull(apiClient);

        this.permissionId = requireNonNull(permissionId);
        this.connectionId = requireNonNull(request.connectionId());
        this.start = requireNonNull(request.start());
        this.end = requireNonNull(request.end());
        this.credentials = new ApiCredentials(apiClient, requireNonNull(request.refreshToken()));
        this.meteringPoint = requireNonNull(request.meteringPoint());
        this.dataNeedId = requireNonNull(request.dataNeedId());
        this.granularity = requireNonNull(request.granularity());

        this.state = new EnerginetCustomerCreatedState(this);
    }

    @Override
    public String permissionId() {
        return permissionId;
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
    public PermissionRequestState state() {
        return state;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
    }

    @Override
    public void changeState(PermissionRequestState state) {
        this.state = state;
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
    public Mono<String> accessToken() {
        return credentials.accessToken();
    }

    @Override
    public Granularity granularity() {
        return granularity;
    }

    @Override
    public String meteringPoint() {
        return meteringPoint;
    }
}