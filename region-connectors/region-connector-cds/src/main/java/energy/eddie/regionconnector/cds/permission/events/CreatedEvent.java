// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.permission.events;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.requests.CdsDataSourceInformation;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

import java.time.ZonedDateTime;

@Entity(name = "CdsCreatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class CreatedEvent extends PersistablePermissionEvent {
    private final String connectionId;
    private final String dataNeedId;
    @Embedded
    private final CdsDataSourceInformation dataSourceInformation;

    public CreatedEvent(String permissionId, String connectionId, String dataNeedId, long cdsServerId) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.dataSourceInformation = new CdsDataSourceInformation(cdsServerId);
    }

    public CreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            long cdsServerId,
            ZonedDateTime created
    ) {
        super(permissionId, PermissionProcessStatus.CREATED, created);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.dataSourceInformation = new CdsDataSourceInformation(cdsServerId);
    }

    protected CreatedEvent() {
        connectionId = null;
        dataNeedId = null;
        dataSourceInformation = null;
    }

    public DataSourceInformation getDataSourceInformation() {
        return dataSourceInformation;
    }

    public String connectionId() {
        return connectionId;
    }

    public String dataNeedId() {
        return dataNeedId;
    }
}
