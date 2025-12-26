package energy.eddie.regionconnector.de.eta.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Permission request implementation for Germany (ETA Plus).
 * This class represents a permission request in the German energy market.
 */
public class DePermissionRequest implements MeterReadingPermissionRequest {
    private final String permissionId;
    private final String connectionId;
    private final String meteringPointId;
    private final LocalDate start;
    private final LocalDate end;
    private final Granularity granularity;
    private final EnergyType energyType;
    private final PermissionProcessStatus status;
    private final ZonedDateTime created;
    private final DataSourceInformation dataSourceInformation;
    private final String dataNeedId;
    
    @Nullable
    private final LocalDate latestMeterReadingEndDate;
    
    @Nullable
    private final String message;
    
    @Nullable
    private final String cause;

    @SuppressWarnings("NullAway") // Builder ensures all required fields are set
    private DePermissionRequest(Builder builder) {
        this.permissionId = builder.permissionId;
        this.connectionId = builder.connectionId;
        this.meteringPointId = builder.meteringPointId;
        this.start = builder.start;
        this.end = builder.end;
        this.granularity = builder.granularity;
        this.energyType = builder.energyType;
        this.status = builder.status;
        this.created = builder.created;
        this.dataSourceInformation = builder.dataSourceInformation;
        this.dataNeedId = builder.dataNeedId;
        this.latestMeterReadingEndDate = builder.latestMeterReadingEndDate;
        this.message = builder.message;
        this.cause = builder.cause;
    }

    public static Builder builder() {
        return new Builder();
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

    /**
     * Builder for DePermissionRequest
     */
    @SuppressWarnings("NullAway") // Builder pattern allows null until build() is called
    public static class Builder {
        @Nullable
        private String permissionId;
        @Nullable
        private String connectionId;
        @Nullable
        private String meteringPointId;
        @Nullable
        private LocalDate start;
        @Nullable
        private LocalDate end;
        @Nullable
        private Granularity granularity;
        @Nullable
        private EnergyType energyType;
        @Nullable
        private PermissionProcessStatus status;
        @Nullable
        private ZonedDateTime created;
        @Nullable
        private DataSourceInformation dataSourceInformation;
        @Nullable
        private String dataNeedId;
        @Nullable
        private LocalDate latestMeterReadingEndDate;
        @Nullable
        private String message;
        @Nullable
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

        public Builder dataSourceInformation(DataSourceInformation dataSourceInformation) {
            this.dataSourceInformation = dataSourceInformation;
            return this;
        }

        public Builder dataNeedId(String dataNeedId) {
            this.dataNeedId = dataNeedId;
            return this;
        }

        public Builder latestMeterReadingEndDate(LocalDate latestMeterReadingEndDate) {
            this.latestMeterReadingEndDate = latestMeterReadingEndDate;
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
            return new DePermissionRequest(this);
        }
    }
}
