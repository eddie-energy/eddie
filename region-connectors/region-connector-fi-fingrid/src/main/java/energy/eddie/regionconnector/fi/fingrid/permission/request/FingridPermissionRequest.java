// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.permission.FingridDataSourceInformation;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.oldestDateTime;

@Entity
@Table(schema = "fi_fingrid", name = "permission_request")
@SuppressWarnings({"NullAway", "unused"})
public class FingridPermissionRequest implements MeterReadingPermissionRequest {
    @Id
    @Column(length = 36)
    private final String permissionId;
    private final String connectionId;
    @Column(length = 36)
    private final String dataNeedId;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final PermissionProcessStatus status;
    private final ZonedDateTime created;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;
    private final String customerIdentification;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyJoinColumn(name = "permission_id", referencedColumnName = "permission_id")
    @CollectionTable(name = "last_meter_readings", joinColumns = @JoinColumn(name = "permission_id"), schema = "fi_fingrid")
    private final Map<String, ZonedDateTime> lastMeterReadings;

    // Too many parameters, but the permission requests require those
    @SuppressWarnings("java:S107")
    FingridPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            ZonedDateTime created,
            LocalDate start,
            LocalDate end,
            String customerIdentification,
            Granularity granularity,
            Map<String, ZonedDateTime> lastMeterReadings
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.created = created;
        this.start = start;
        this.end = end;
        this.customerIdentification = customerIdentification;
        this.granularity = granularity;
        this.lastMeterReadings = lastMeterReadings;
    }

    protected FingridPermissionRequest() {
        permissionId = null;
        connectionId = null;
        dataNeedId = null;
        status = null;
        created = null;
        start = null;
        end = null;
        customerIdentification = null;
        granularity = null;
        lastMeterReadings = Map.of();
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
        return new FingridDataSourceInformation();
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

    public String customerIdentification() {
        return customerIdentification;
    }

    public Granularity granularity() {
        return granularity;
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return oldestDateTime(lastMeterReadings.values()).map(ZonedDateTime::toLocalDate);
    }

    public Optional<ZonedDateTime> latestMeterReading(String meterEAN) {
        return Optional.ofNullable(lastMeterReadings.get(meterEAN));
    }

    public Set<String> meterEANs() {
        return lastMeterReadings.keySet();
    }
}
