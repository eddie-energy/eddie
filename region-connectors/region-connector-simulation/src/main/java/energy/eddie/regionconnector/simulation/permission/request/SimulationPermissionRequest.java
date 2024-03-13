package energy.eddie.regionconnector.simulation.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.TerminalPermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.simulation.SimulationDataSourceInformation;
import energy.eddie.regionconnector.simulation.dtos.SetConnectionStatusRequest;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public record SimulationPermissionRequest(SetConnectionStatusRequest req) implements TimeframedPermissionRequest {

    @Override
    public ZonedDateTime start() {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.UTC);
    }

    @Override
    public ZonedDateTime end() {
        return ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    }

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
}