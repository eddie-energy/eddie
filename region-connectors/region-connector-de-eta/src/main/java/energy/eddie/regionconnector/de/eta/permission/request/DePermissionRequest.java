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
    @Column(name = "latest_reading")
    private ZonedDateTime latestReading;

    @Nullable
    private final String message;

    @Nullable
    private final String cause;

    @SuppressWarnings("java:S107")
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
            @Nullable ZonedDateTime latestReading,
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
        this.latestReading = latestReading;
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
        this.latestReading = null;
        this.message = null;
        this.cause = null;
    }

    public void setLatestReading(@Nullable ZonedDateTime latestReading) {
        this.latestReading = latestReading;
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
        if (latestReading == null) {
            return Optional.empty();
        }
        return Optional.of(latestReading.toLocalDate());
    }

    public Optional<ZonedDateTime> latestReading() {
        return Optional.ofNullable(latestReading);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String permissionId;
        private String connectionId;
        private String meteringPointId;
        private LocalDate start;
        private LocalDate end;
        private Granularity granularity;
        private EnergyType energyType;
        private PermissionProcessStatus status;
        private ZonedDateTime created;
        private String dataNeedId;
        private ZonedDateTime latestReading;
        private String message;
        private String cause;

        public Builder permissionId(String permissionId) {
            this.permissionId = permissionId;
            return this;
        }

        public Builder connectionId(String connectionId) {
            this.connectionId = connectionId;
            return this;
        }

        public Builder meteringPointId(String meteringPointId) {
            this.meteringPointId = meteringPointId;
            return this;
        }

        public Builder start(LocalDate start) {
            this.start = start;
            return this;
        }

        public Builder end(LocalDate end) {
            this.end = end;
            return this;
        }

        public Builder granularity(Granularity granularity) {
            this.granularity = granularity;
            return this;
        }

        public Builder energyType(EnergyType energyType) {
            this.energyType = energyType;
            return this;
        }

        public Builder status(PermissionProcessStatus status) {
            this.status = status;
            return this;
        }

        public Builder created(ZonedDateTime created) {
            this.created = created;
            return this;
        }

        public Builder dataNeedId(String dataNeedId) {
            this.dataNeedId = dataNeedId;
            return this;
        }

        public Builder latestReading(@Nullable ZonedDateTime latestReading) {
            this.latestReading = latestReading;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder cause(String cause) {
            this.cause = cause;
            return this;
        }

        public DePermissionRequest build() {
            return new DePermissionRequest(
                    permissionId, connectionId, meteringPointId, start, end,
                    granularity, energyType, status, created, dataNeedId,
                    latestReading, message, cause
            );
        }
    }
}