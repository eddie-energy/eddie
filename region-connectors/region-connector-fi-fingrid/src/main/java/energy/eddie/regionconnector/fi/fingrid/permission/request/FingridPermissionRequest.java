package energy.eddie.regionconnector.fi.fingrid.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.permission.FingridDataSourceInformation;
import energy.eddie.regionconnector.shared.services.CommonPermissionRequest;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

@Entity
@Table(schema = "fi_fingrid", name = "permission_request")
@SuppressWarnings({"NullAway", "unused"})
public class FingridPermissionRequest implements CommonPermissionRequest {
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
    @Column(name = "metering_point")
    private final String meteringPointEAN;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;
    @Nullable
    private final ZonedDateTime latestMeterReading;

    // Too many parameters, but the permission requests require those
    @SuppressWarnings("java:S107")
    public FingridPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            ZonedDateTime created,
            LocalDate start,
            LocalDate end,
            String customerIdentification,
            String meteringPointEAN,
            Granularity granularity,
            @Nullable ZonedDateTime latestMeterReading
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.created = created;
        this.start = start;
        this.end = end;
        this.customerIdentification = customerIdentification;
        this.meteringPointEAN = meteringPointEAN;
        this.granularity = granularity;
        this.latestMeterReading = latestMeterReading;
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
        meteringPointEAN = null;
        granularity = null;
        latestMeterReading = null;
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

    public String meteringPointEAN() {
        return meteringPointEAN;
    }

    public Granularity granularity() {
        return granularity;
    }

    public Optional<ZonedDateTime> latestMeterReading() {
        return Optional.ofNullable(latestMeterReading);
    }

    @Override
    public String usagePointId() {
        return "";
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return Optional.empty();
    }
}
