package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerCreatedState;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;

import java.time.ZonedDateTime;
import java.util.Objects;

public class EnerginetCustomerPermissionRequest implements DkEnerginetCustomerPermissionRequest {
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
            String connectionId,
            ZonedDateTime start,
            ZonedDateTime end,
            String refreshToken,
            String meteringPoint,
            String dataNeedId,
            PeriodResolutionEnum periodResolution,
            EnerginetConfiguration configuration) {
        Objects.requireNonNull(configuration);

        this.permissionId = Objects.requireNonNull(permissionId);
        this.connectionId = Objects.requireNonNull(connectionId);
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);
        this.refreshToken = Objects.requireNonNull(refreshToken);
        this.meteringPoint = Objects.requireNonNull(meteringPoint);
        this.dataNeedId = Objects.requireNonNull(dataNeedId);
        this.periodResolution = Objects.requireNonNull(periodResolution);

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
