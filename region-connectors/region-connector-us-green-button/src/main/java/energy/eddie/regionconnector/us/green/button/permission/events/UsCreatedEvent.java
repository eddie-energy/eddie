// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.permission.GreenButtonDataSourceInformation;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

import java.time.ZonedDateTime;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class UsCreatedEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String connectionId;
    @Column(length = 36)
    private final String dataNeedId;
    @Column(columnDefinition = "text")
    private final String jumpOffUrl;
    @Embedded
    private final GreenButtonDataSourceInformation dataSourceInformation;

    public UsCreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String jumpOffUrl,
            GreenButtonDataSourceInformation dataSourceInformation
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.jumpOffUrl = jumpOffUrl;
        this.dataSourceInformation = dataSourceInformation;
    }

    public UsCreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String jumpOffUrl,
            GreenButtonDataSourceInformation dataSourceInformation,
            ZonedDateTime created
    ) {
        super(permissionId, PermissionProcessStatus.CREATED, created);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.jumpOffUrl = jumpOffUrl;
        this.dataSourceInformation = dataSourceInformation;
    }

    protected UsCreatedEvent() {
        super();
        connectionId = null;
        dataNeedId = null;
        jumpOffUrl = null;
        dataSourceInformation = null;
    }
}
