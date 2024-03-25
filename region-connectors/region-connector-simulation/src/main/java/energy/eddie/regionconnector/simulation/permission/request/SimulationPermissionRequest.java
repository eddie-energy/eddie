package energy.eddie.regionconnector.simulation.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.TerminalPermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.simulation.SimulationDataSourceInformation;
import energy.eddie.regionconnector.simulation.dtos.SetConnectionStatusRequest;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public record SimulationPermissionRequest(SetConnectionStatusRequest req) implements PermissionRequest {

    @Nullable
    @Override
    public String permissionId() {
        return req.connectionId;
    }

    @Nullable
    @Override
    public String connectionId() {
        return req.connectionId;
    }

    @Nullable
    @Override
    public String dataNeedId() {
        return req.dataNeedId;
    }

    @Override
    public PermissionRequestState state() {
        return (TerminalPermissionRequestState) () -> req.connectionStatus;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return new SimulationDataSourceInformation();
    }

    @Override
    public ZonedDateTime created() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    @Override
    public void changeState(PermissionRequestState state) {
        // No-Op
    }

    @Override
    public LocalDate start() {
        return LocalDate.of(2021, 1, 1);
    }

    @Override
    public LocalDate end() {
        return LocalDate.of(9999, 12, 31);
    }
}
