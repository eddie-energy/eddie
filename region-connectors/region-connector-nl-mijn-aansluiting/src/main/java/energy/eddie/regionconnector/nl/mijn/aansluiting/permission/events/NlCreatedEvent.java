// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.MijnAansluitingDataSourceInformation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import java.time.ZonedDateTime;

@Entity
@SuppressWarnings("NullAway")
public class NlCreatedEvent extends NlPermissionEvent {
    @Column(columnDefinition = "text")
    private final String connectionId;
    @Column(length = 36)
    private final String dataNeedId;
    @Transient
    private final MijnAansluitingDataSourceInformation dataSourceInformation;
    private final ZonedDateTime created;

    protected NlCreatedEvent() {
        this(null, null, null, null);
    }


    public NlCreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            ZonedDateTime created
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.dataSourceInformation = new MijnAansluitingDataSourceInformation();
        this.created = created;
    }

    public String connectionId() {
        return connectionId;
    }

    public String dataNeedId() {
        return dataNeedId;
    }

    public DataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
    }

    public ZonedDateTime created() {
        return created;
    }
}
