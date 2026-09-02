// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity(name = "AtImportEvent")
public class ImportEvent extends PersistablePermissionEvent {
    @Column
    private final String connectionId;
    @Column
    private final String meteringPointId;
    @Column
    private final String dataNeedId;
    @Embedded
    private final EdaDataSourceInformation dataSourceInformation;
    @Column
    private final String cmConsentId;
    @Column
    private final LocalDate permissionStart;
    @Nullable
    @Column
    private final LocalDate permissionEnd;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final AllowedGranularity granularity;
    @Column
    private final ZonedDateTime created;
    @Column(name = "meter_reading_start")
    @Nullable
    private final ZonedDateTime meterReadingStart;
    @Column(name = "meter_reading_end")
    @Nullable
    private final ZonedDateTime meterReadingEnd;

    @SuppressWarnings("java:S107")
    public ImportEvent(
            String permissionId,
            String connectionId,
            String meteringPointId,
            String dataNeedId,
            String dsoId,
            String cmConsentId,
            LocalDate permissionStart,
            @Nullable LocalDate permissionEnd,
            AllowedGranularity granularity,
            ZonedDateTime created,
            @Nullable
            ZonedDateTime meterReadingStart,
            @Nullable
            ZonedDateTime meterReadingEnd
    ) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.connectionId = connectionId;
        this.meteringPointId = meteringPointId;
        this.dataNeedId = dataNeedId;
        this.dataSourceInformation = new EdaDataSourceInformation(dsoId);
        this.cmConsentId = cmConsentId;
        this.permissionStart = permissionStart;
        this.permissionEnd = permissionEnd;
        this.granularity = granularity;
        this.created = created;
        this.meterReadingStart = meterReadingStart;
        this.meterReadingEnd = meterReadingEnd;
    }

    @SuppressWarnings("NullAway")
    protected ImportEvent() {
        this.connectionId = null;
        this.meteringPointId = null;
        this.dataNeedId = null;
        this.dataSourceInformation = null;
        this.cmConsentId = null;
        this.permissionStart = null;
        this.permissionEnd = null;
        this.granularity = null;
        this.created = null;
        this.meterReadingStart = null;
        this.meterReadingEnd = null;
    }

    public String connectionId() {
        return connectionId;
    }

    public String meteringPointId() {
        return meteringPointId;
    }

    public String dataNeedId() {
        return dataNeedId;
    }

    public EdaDataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
    }

    public String cmConsentId() {
        return cmConsentId;
    }

    public LocalDate permissionStart() {
        return permissionStart;
    }

    @Nullable
    public LocalDate permissionEnd() {
        return permissionEnd;
    }

    public AllowedGranularity granularity() {
        return granularity;
    }

    public ZonedDateTime created() {
        return created;
    }

    @Nullable
    public ZonedDateTime meterReadingStart() {
        return meterReadingStart;
    }

    @Nullable
    public ZonedDateTime meterReadingEnd() {
        return meterReadingEnd;
    }
}
