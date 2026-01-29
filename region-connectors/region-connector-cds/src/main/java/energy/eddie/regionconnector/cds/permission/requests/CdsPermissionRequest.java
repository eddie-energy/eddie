// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.permission.requests;

import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.oldestDateTime;

@Entity
@Table(name = "permission_request", schema = "cds")
public class CdsPermissionRequest implements MeterReadingPermissionRequest {
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
    @Embedded
    private final CdsDataSourceInformation dataSourceInformation;
    @Column(name = "created")
    private final ZonedDateTime created;
    @Column(name = "data_start")
    private final LocalDate dataStart;
    @Column(name = "data_end")
    private final LocalDate dataEnd;
    @Column(name = "state")
    @SuppressWarnings("unused")
    private final String state;
    @Column(name = "redirect_uri")
    @Nullable
    private final String redirectUri;
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyJoinColumn(name = "permission_id", referencedColumnName = "permission_id")
    @CollectionTable(name = "last_meter_readings", joinColumns = @JoinColumn(name = "permission_id"), schema = "cds")
    private final Map<String, ZonedDateTime> lastMeterReadings;


    @SuppressWarnings("java:S107")
    public CdsPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            long cdsServer,
            ZonedDateTime created,
            LocalDate dataStart,
            LocalDate dataEnd,
            String state,
            @Nullable String redirectUri,
            Map<String, ZonedDateTime> lastMeterReadings
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.created = created;
        this.dataStart = dataStart;
        this.dataEnd = dataEnd;
        dataSourceInformation = new CdsDataSourceInformation(cdsServer);
        this.state = state;
        this.redirectUri = redirectUri;
        this.lastMeterReadings = lastMeterReadings;
    }

    @SuppressWarnings("NullAway")
    protected CdsPermissionRequest() {
        permissionId = null;
        connectionId = null;
        dataNeedId = null;
        status = null;
        dataSourceInformation = null;
        created = null;
        dataStart = null;
        dataEnd = null;
        state = null;
        redirectUri = null;
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
    public CdsDataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
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

    public Optional<String> redirectUri() {
        return Optional.ofNullable(redirectUri);
    }

    public Optional<ZonedDateTime> oldestMeterReading() {
        return oldestDateTime(lastMeterReadings.values());
    }

    public Map<String, ZonedDateTime> lastMeterReadings() {
        return lastMeterReadings;
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return oldestMeterReading().map(ZonedDateTime::toLocalDate);
    }
}
