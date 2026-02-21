package energy.eddie.regionconnector.de.eta.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.DeDataSourceInformation;
import jakarta.persistence.*;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Permission request implementation for Germany (ETA Plus).
 * This class represents a permission request in the German energy market.
 */
@Entity
@Table(schema = "de_eta", name = "eta_permission_request")
@SuppressWarnings({"NullAway", "unused"})
public class DePermissionRequest implements MeterReadingPermissionRequest {
    @Transient
    private final DataSourceInformation dataSourceInformation = new DeDataSourceInformation();

    @Id
    @Column(name = "permission_id")
    private final String permissionId;

    @Column(name = "data_source_connection_id")
    private final String connectionId;

    @Column(name = "metering_point_id")
    private final String meteringPointId;

    @Column(name = "data_start")
    private final LocalDate start;

    @Column(name = "data_end")
    private final LocalDate end;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_type", columnDefinition = "text")
    private final EnergyType energyType;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final PermissionProcessStatus status;

    private final ZonedDateTime created;

    @Column(name = "data_need_id")
    private final String dataNeedId;

    @Nullable
    @Column(name = "latest_meter_reading")
    private final LocalDate latestMeterReadingEndDate;

    @Nullable
    private final String message;

    @Nullable
    private final String cause;

    @SuppressWarnings("java:S107") // Constructor with many parameters is needed for mapping
    public DePermissionRequest(
            String permissionId,
            String connectionId,
            String meteringPointId,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            EnergyType energyType,
            PermissionProcessStatus status,
            ZonedDateTime created,
            String dataNeedId,
            @Nullable LocalDate latestMeterReadingEndDate,
            @Nullable String message,
            @Nullable String cause
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.meteringPointId = meteringPointId;
        this.start = start;
        this.end = end;
        this.granularity = granularity;
        this.energyType = energyType;
        this.status = status;
        this.created = created;
        this.dataNeedId = dataNeedId;
        this.latestMeterReadingEndDate = latestMeterReadingEndDate;
        this.message = message;
        this.cause = cause;
    }

    protected DePermissionRequest() {
        this.permissionId = null;
        this.connectionId = null;
        this.meteringPointId = null;
        this.start = null;
        this.end = null;
        this.granularity = null;
        this.energyType = null;
        this.status = null;
        this.created = null;
        this.dataNeedId = null;
        this.latestMeterReadingEndDate = null;
        this.message = null;
        this.cause = null;
    }

    @Override
    public String permissionId() {
        return permissionId;
    }

    @Override
    public String connectionId() {
        return connectionId;
    }

    public String meteringPointId() {
        return meteringPointId;
    }

    @Override
    public LocalDate start() {
        return start;
    }

    @Override
    public LocalDate end() {
        return end;
    }

    public Granularity granularity() {
        return granularity;
    }

    public EnergyType energyType() {
        return energyType;
    }

    @Override
    public PermissionProcessStatus status() {
        return status;
    }

    @Override
    public ZonedDateTime created() {
        return created;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
    }

    @Override
    public String dataNeedId() {
        return dataNeedId;
    }

    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    public Optional<String> cause() {
        return Optional.ofNullable(cause);
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return Optional.ofNullable(latestMeterReadingEndDate);
    }
}
