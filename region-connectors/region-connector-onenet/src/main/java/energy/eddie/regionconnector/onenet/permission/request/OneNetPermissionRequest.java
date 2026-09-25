// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.agnostic.DataSourceInformation;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;


@Entity
@Table(name = "permission_request", schema = "onenet")
public class OneNetPermissionRequest implements PermissionRequest {
    @Id
    @Column(name = "permission_id")
    private final String permissionId;
    @Column(name = "connection_id")
    private final String connectionId;
    @Column(name = "data_need_id")
    private final String dataNeedId;
    @Column(name = "status", columnDefinition = "text")
    @Enumerated(EnumType.STRING)
    private final PermissionProcessStatus status;
    @Column(name = "created")
    private final ZonedDateTime created;
    @Column(name = "data_start")
    private final LocalDate dataStart;
    @Column(name = "data_end")
    private final LocalDate dataEnd;

    OneNetPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            ZonedDateTime created,
            LocalDate dataStart,
            LocalDate dataEnd
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.created = created;
        this.dataStart = dataStart;
        this.dataEnd = dataEnd;
    }

    @SuppressWarnings("NullAway")
    protected OneNetPermissionRequest() {
        permissionId = null;
        connectionId = null;
        dataNeedId = null;
        status = null;
        created = null;
        dataStart = null;
        dataEnd = null;
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
        return new OneNetDataSourceInformation();
    }

    @Override
    public ZonedDateTime created() {
        return created;
    }

    @Override
    public LocalDate start() {
        return dataStart;
    }

    @Override
    public LocalDate end() {
        return dataEnd;
    }
}
