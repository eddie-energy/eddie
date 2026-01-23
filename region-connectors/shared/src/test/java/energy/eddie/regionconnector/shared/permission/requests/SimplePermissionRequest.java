// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.permission.requests;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public final class SimplePermissionRequest implements PermissionRequest {
    private final String permissionId;
    private final String connectionId;
    private final String dataNeedId;
    private final LocalDate start;
    private final LocalDate end;
    private final ZonedDateTime created;
    private final PermissionProcessStatus status;

    public SimplePermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        start = null;
        end = null;
        created = null;
        this.status = status;
    }

    public SimplePermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            LocalDate start,
            LocalDate end,
            ZonedDateTime created,
            PermissionProcessStatus status
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.start = start;
        this.end = end;
        this.created = created;
        this.status = status;
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
    public DataSourceInformation dataSourceInformation() {
        return new DummyDataSourceInformation();
    }

    @Override
    public ZonedDateTime created() {
        return created;
    }

    @Override
    public LocalDate start() {
        return start;
    }

    @Override
    public LocalDate end() {
        return end;
    }
}
