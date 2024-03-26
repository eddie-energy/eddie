package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.TimestampedPermissionRequest;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Optional;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static java.util.Objects.requireNonNull;

@Entity
@Table(schema = "dk_energinet", name = "energinet_customer_permission_request")
public class EnerginetCustomerPermissionRequest extends TimestampedPermissionRequest implements DkEnerginetCustomerPermissionRequest {
    private static final EnerginetDataSourceInformation dataSourceInformation = new EnerginetDataSourceInformation();
    @Id
    private String permissionId;
    private String connectionId;
    @Column(name = "start_date")
    private LocalDate start;
    @Column(name = "end_date")
    private LocalDate end;
    @Transient
    private ApiCredentials credentials;
    private String meteringPoint;
    private String dataNeedId;
    @Enumerated(EnumType.STRING)
    private Granularity granularity;
    @Transient
    private PermissionRequestState state;
    @Column(name = "latest_meter_reading_end_date")
    @Nullable
    private LocalDate latestMeterReadingEndDate;
    @Column(columnDefinition = "TEXT")
    private String refreshToken;
    @Enumerated(EnumType.STRING)
    private PermissionProcessStatus status;

    // just for JPA
    @SuppressWarnings("NullAway.Init")
    protected EnerginetCustomerPermissionRequest() {
        super(DK_ZONE_ID);
    }

    public EnerginetCustomerPermissionRequest(
            String permissionId,
            PermissionRequestForCreation request,
            EnerginetCustomerApi apiClient,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            StateBuilderFactory factory
    ) {
        super(DK_ZONE_ID);
        requireNonNull(permissionId);
        requireNonNull(request);
        requireNonNull(apiClient);

        this.permissionId = requireNonNull(permissionId);
        this.connectionId = requireNonNull(request.connectionId());
        this.credentials = new ApiCredentials(apiClient, requireNonNull(request.refreshToken()));
        this.meteringPoint = requireNonNull(request.meteringPoint());
        this.dataNeedId = requireNonNull(request.dataNeedId());
        this.refreshToken = request.refreshToken();
        this.start = start;
        this.end = end;
        this.granularity = granularity;

        this.state = factory.create(this, PermissionProcessStatus.CREATED).build();
        this.status = state.status();
    }

    @Override
    public DkEnerginetCustomerPermissionRequest withApiClient(EnerginetCustomerApi client) {
        this.credentials = new ApiCredentials(client, refreshToken);
        return this;
    }

    @Override
    public DkEnerginetCustomerPermissionRequest withStateBuilderFactory(StateBuilderFactory factory) {
        this.state = factory
                .create(this, status)
                .build();
        return this;
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
    public PermissionProcessStatus status() {
        return status;
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
    public LocalDate start() {
        return start;
    }

    @Override
    public LocalDate end() {
        return end;
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return Optional.ofNullable(latestMeterReadingEndDate);
    }

    @Override
    public void updateLatestMeterReadingEndDate(LocalDate date) {
        this.latestMeterReadingEndDate = date;
    }
}
