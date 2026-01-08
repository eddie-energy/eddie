package energy.eddie.regionconnector.de.eta.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;

import org.springframework.lang.Nullable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Test builder for DePermissionRequest.
 * This builder is only used in tests since production instances are created by JPA.
 */
@SuppressWarnings("NullAway") // Builder pattern allows null until build() is called
public class DePermissionRequestBuilder {
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
    private String dataNeedId;
    @Nullable
    private ZonedDateTime latestMeterReadingEndDate;
    @Nullable
    private String message;
    @Nullable
    private String cause;

    public DePermissionRequestBuilder permissionId(String permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public DePermissionRequestBuilder connectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public DePermissionRequestBuilder meteringPointId(String meteringPointId) {
        this.meteringPointId = meteringPointId;
        return this;
    }

    public DePermissionRequestBuilder start(LocalDate start) {
        this.start = start;
        return this;
    }

    public DePermissionRequestBuilder end(LocalDate end) {
        this.end = end;
        return this;
    }

    public DePermissionRequestBuilder granularity(Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    public DePermissionRequestBuilder energyType(EnergyType energyType) {
        this.energyType = energyType;
        return this;
    }

    public DePermissionRequestBuilder status(PermissionProcessStatus status) {
        this.status = status;
        return this;
    }

    public DePermissionRequestBuilder created(ZonedDateTime created) {
        this.created = created;
        return this;
    }

    public DePermissionRequestBuilder dataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
        return this;
    }

    public DePermissionRequestBuilder latestMeterReadingEndDate(LocalDate localDate) {
        if (localDate != null) {
            this.latestMeterReadingEndDate = localDate.atStartOfDay(ZoneId.of("UTC"));
        } else {
            this.latestMeterReadingEndDate = null;
        }
        return this;
    }

    public DePermissionRequestBuilder latestReading(ZonedDateTime latestReading) {
        this.latestMeterReadingEndDate = latestReading;
        return this;
    }

    public DePermissionRequestBuilder message(String message) {
        this.message = message;
        return this;
    }

    public DePermissionRequestBuilder cause(String cause) {
        this.cause = cause;
        return this;
    }

    public DePermissionRequest build() {
        return new DePermissionRequest(
                permissionId,
                connectionId,
                meteringPointId,
                start,
                end,
                granularity,
                energyType,
                status,
                created,
                dataNeedId,
                latestMeterReadingEndDate,
                message,
                cause
        );
    }
}
