package energy.eddie.regionconnector.si.moj.elektro.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.si.moj.elektro.permission.MojElektroDataSourceInformation;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

@Entity
@Table(name = "permission_request", schema = "si_moj_elektro")
@SuppressWarnings("NullAway")
public class MojElektroPermissionRequest implements MeterReadingPermissionRequest {

    private static final MojElektroDataSourceInformation dataSourceInformation = new MojElektroDataSourceInformation();

    @Id
    @Column(length = 36)
    private final String permissionId;

    private final String connectionId;

    @Column(length = 36)
    private final String dataNeedId;

    @Enumerated(EnumType.STRING)
    private final PermissionProcessStatus status;

    private final Granularity granularity;

    @Column(name = "permission_start")
    private final LocalDate start;

    @Column(name = "permission_end")
    private final LocalDate end;

    @Column(name = "event_created")
    private final ZonedDateTime created;

    @Nullable
    private final LocalDate latestMeterReadingEndDate;

    @Nullable
    private final String apiToken;

    @Nullable
    private final String meteringPoint;

    public MojElektroPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            Granularity granularity,
            LocalDate start,
            LocalDate end,
            ZonedDateTime created,
            @Nullable LocalDate latestMeterReadingEndDate,
            @Nullable String apiToken,
            @Nullable String meteringPoint
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.granularity = granularity;
        this.start = start;
        this.end = end;
        this.created = created;
        this.latestMeterReadingEndDate = latestMeterReadingEndDate;
        this.apiToken = apiToken;
        this.meteringPoint = meteringPoint;
    }

    protected MojElektroPermissionRequest() {
        permissionId = null;
        connectionId = null;
        dataNeedId = null;
        status = null;
        granularity = null;
        start = null;
        end = null;
        created = null;
        latestMeterReadingEndDate = null;
        apiToken = null;
        meteringPoint = null;
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return Optional.ofNullable(latestMeterReadingEndDate);
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
        return dataSourceInformation;
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

    public Granularity granularity() {
        return granularity;
    }

    public String apiToken() {
        return apiToken;
    }

    public String meteringPoint() {
        return meteringPoint;
    }
}
