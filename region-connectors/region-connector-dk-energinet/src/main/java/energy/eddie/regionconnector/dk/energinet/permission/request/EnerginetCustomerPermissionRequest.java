package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.TimestampedPermissionRequest;
import jakarta.persistence.*;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.DK_ZONE_ID;
import static java.util.Objects.requireNonNull;

@Entity
@Table(schema = "dk_energinet")
public class EnerginetCustomerPermissionRequest extends TimestampedPermissionRequest implements DkEnerginetCustomerPermissionRequest {
    private static final EnerginetDataSourceInformation dataSourceInformation = new EnerginetDataSourceInformation();
    @Id
    private String permissionId;
    private String connectionId;
    @Column(name = "start_timestamp")
    private ZonedDateTime start;
    @Column(name = "end_timestamp")
    private ZonedDateTime end;
    @Transient
    private ApiCredentials credentials;
    private String meteringPoint;
    private String dataNeedId;
    @Enumerated(EnumType.STRING)
    private Granularity granularity;
    @Transient
    private PermissionRequestState state;
    private ZonedDateTime lastPolled;
    @Column(columnDefinition = "TEXT")
    private String refreshToken;
    @Enumerated(EnumType.STRING)
    private PermissionProcessStatus status;

    // just for JPA
    @SuppressWarnings("NullAway.Init")
    protected EnerginetCustomerPermissionRequest() {
        super(DK_ZONE_ID);
    }

    @Override
    public EnerginetCustomerPermissionRequest withApiClient(EnerginetCustomerApi client) {
        this.credentials = new ApiCredentials(client, refreshToken);
        return this;
    }

    @Override
    public EnerginetCustomerPermissionRequest withStateBuilderFactory(StateBuilderFactory factory) {
        this.state = factory
                .create(this, status)
                .build();
        return this;
    }

    public EnerginetCustomerPermissionRequest(
            String permissionId,
            PermissionRequestForCreation request,
            EnerginetCustomerApi apiClient,
            StateBuilderFactory factory) {
        super(DK_ZONE_ID);
        requireNonNull(permissionId);
        requireNonNull(request);
        requireNonNull(apiClient);

        this.permissionId = requireNonNull(permissionId);
        this.connectionId = requireNonNull(request.connectionId());
        this.start = requireNonNull(request.start()).withZoneSameInstant(DK_ZONE_ID);
        this.end = requireNonNull(request.end()).withZoneSameInstant(DK_ZONE_ID);
        this.credentials = new ApiCredentials(apiClient, requireNonNull(request.refreshToken()));
        this.meteringPoint = requireNonNull(request.meteringPoint());
        this.dataNeedId = requireNonNull(request.dataNeedId());
        this.granularity = requireNonNull(request.granularity());
        this.refreshToken = request.refreshToken();

        this.state = factory.create(this, PermissionProcessStatus.CREATED).build();
        this.lastPolled = start;
        this.status = state.status();
    }

    @Override
    public PermissionProcessStatus status() {
        return status;
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
        this.status = state.status();
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

    @Override
    public ZonedDateTime lastPolled() {
        return lastPolled;
    }

    @Override
    public void updateLastPolled(ZonedDateTime lastPolled) {
        this.lastPolled = lastPolled;
    }
}