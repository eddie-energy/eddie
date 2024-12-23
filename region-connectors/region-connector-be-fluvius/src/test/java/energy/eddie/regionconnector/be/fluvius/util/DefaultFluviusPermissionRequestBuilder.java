package energy.eddie.regionconnector.be.fluvius.util;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class DefaultFluviusPermissionRequestBuilder {
    String permissionId = "pid";
    String connectionId = "cid";
    String dataNeedId = "did";
    PermissionProcessStatus status = PermissionProcessStatus.CREATED;
    Granularity granularity = Granularity.PT15M;
    LocalDate start = LocalDate.now(ZoneOffset.UTC);
    LocalDate end = LocalDate.now(ZoneOffset.UTC);
    ZonedDateTime created = ZonedDateTime.now(ZoneOffset.UTC);
    Flow flow = Flow.B2C;
    @Nullable
    String shortUrlIdentifier = null;
    private final List<MeterReading> meterReadings = new ArrayList<>();

    public static DefaultFluviusPermissionRequestBuilder create() {
        return new DefaultFluviusPermissionRequestBuilder();
    }

    public DefaultFluviusPermissionRequestBuilder permissionId(String permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public DefaultFluviusPermissionRequestBuilder connectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public DefaultFluviusPermissionRequestBuilder dataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
        return this;
    }

    public DefaultFluviusPermissionRequestBuilder status(PermissionProcessStatus status) {
        this.status = status;
        return this;
    }

    public DefaultFluviusPermissionRequestBuilder granularity(Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    public DefaultFluviusPermissionRequestBuilder start(LocalDate start) {
        this.start = start;
        return this;
    }

    public DefaultFluviusPermissionRequestBuilder end(LocalDate end) {
        this.end = end;
        return this;
    }

    public DefaultFluviusPermissionRequestBuilder created(ZonedDateTime created) {
        this.created = created;
        return this;
    }

    public DefaultFluviusPermissionRequestBuilder flow(Flow flow) {
        this.flow = flow;
        return this;
    }

    public DefaultFluviusPermissionRequestBuilder shortUrlIdentifier(@Nullable String shortUrlIdentifier) {
        this.shortUrlIdentifier = shortUrlIdentifier;
        return this;
    }

    public DefaultFluviusPermissionRequestBuilder addMeterReadings(MeterReading meterReading) {
        meterReadings.add(meterReading);
        return this;
    }

    public FluviusPermissionRequest build() {
        return new FluviusPermissionRequest(
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
                meterReadings
        );
    }
}
