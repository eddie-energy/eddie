// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "permission_request", schema = "be_fluvius")
@SuppressWarnings({"NullAway", "unused"})
public class FluviusPermissionRequest implements MeterReadingPermissionRequest {
    @Id
    @Column(name = "permission_id")
    private final String permissionId;
    @Column(name = "connection_id")
    private final String connectionId;
    @Column(name = "data_need_id")
    private final String dataNeedId;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private final PermissionProcessStatus status;
    @Column(name = "granularity")
    @Enumerated(EnumType.STRING)
    @SuppressWarnings("unused")
    private final Granularity granularity;
    @Column(name = "data_start")
    private final LocalDate start;
    @Column(name = "data_end")
    private final LocalDate end;
    @Column(name = "created")
    private final ZonedDateTime created;
    @Column(name = "flow")
    @Enumerated(EnumType.STRING)
    private final Flow flow;
    @Column(name = "short_url_identifier")
    private final String shortUrlIdentifier;
    @OneToMany(fetch = FetchType.EAGER, targetEntity = MeterReading.class)
    @JoinColumn(insertable = false, updatable = false, name = "permission_id", referencedColumnName = "permission_id")
    private final List<MeterReading> meterReadings;

    @SuppressWarnings("java:S107")
    public FluviusPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            Granularity granularity,
            LocalDate start,
            LocalDate end,
            ZonedDateTime created,
            Flow flow
    ) {
        this(
                permissionId,
                connectionId,
                dataNeedId,
                status,
                granularity,
                start,
                end,
                created,
                flow,
                null
        );
    }

    @SuppressWarnings("java:S107")
    public FluviusPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            Granularity granularity,
            LocalDate start,
            LocalDate end,
            ZonedDateTime created,
            Flow flow,
            @Nullable
            String shortUrlIdentifier
    ) {
        this(
                permissionId,
                connectionId,
                dataNeedId,
                status,
                granularity,
                start,
                end,
                created,
                flow,
                shortUrlIdentifier,
                List.of()
        );
    }

    @SuppressWarnings("java:S107")
    public FluviusPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            Granularity granularity,
            LocalDate start,
            LocalDate end,
            ZonedDateTime created,
            Flow flow,
            @Nullable
            String shortUrlIdentifier,
            List<MeterReading> meterReadings
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.granularity = granularity;
        this.start = start;
        this.end = end;
        this.created = created;
        this.flow = flow;
        this.shortUrlIdentifier = shortUrlIdentifier;
        this.meterReadings = meterReadings;
    }

    protected FluviusPermissionRequest() {
        permissionId = null;
        connectionId = null;
        dataNeedId = null;
        status = null;
        granularity = null;
        start = null;
        end = null;
        created = null;
        flow = null;
        shortUrlIdentifier = null;
        meterReadings = List.of();
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
        return new FluviusDataSourceInformation();
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

    public Flow flow() {
        return flow;
    }

    public Granularity granularity() {
        return granularity;
    }

    public String shortUrlIdentifier() {
        return shortUrlIdentifier;
    }

    public List<MeterReading> lastMeterReadings() {
        return meterReadings;
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return DateTimeUtils.oldestDateTime(MeterReading.lastMeterReadingDates(meterReadings))
                            .map(ZonedDateTime::toLocalDate);
    }

    public Set<String> meters() {
        return MeterReading.meters(meterReadings);
    }
}
