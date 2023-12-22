package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerCreatedState;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;

import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

public class EnerginetCustomerPermissionRequest implements DkEnerginetCustomerPermissionRequest {
    private static final EnerginetDataSourceInformation dataSourceInformation = new EnerginetDataSourceInformation();
    private final String permissionId;
    private final String connectionId;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final String refreshToken;
    private final String meteringPoint;
    private final String dataNeedId;
    private final PeriodResolutionEnum periodResolution;
    private PermissionRequestState state;

    public EnerginetCustomerPermissionRequest(
            String permissionId,
            PermissionRequestForCreation request,
            EnerginetConfiguration configuration) {
        requireNonNull(permissionId);
        requireNonNull(request);
        requireNonNull(configuration);

        this.permissionId = requireNonNull(permissionId);
        this.connectionId = requireNonNull(request.connectionId());
        this.start = requireNonNull(request.start());
        this.end = requireNonNull(request.end());
        this.refreshToken = requireNonNull(request.refreshToken());
        this.meteringPoint = requireNonNull(request.meteringPoint());
        this.dataNeedId = requireNonNull(request.dataNeedId());
        this.periodResolution = requireNonNull(request.periodResolution());

        this.state = new EnerginetCustomerCreatedState(this, configuration);
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
    public String refreshToken() {
        return refreshToken;
    }

    @Override
    public PeriodResolutionEnum periodResolution() {
        return periodResolution;
    }

    @Override
    public String meteringPoint() {
        return meteringPoint;
    }
}