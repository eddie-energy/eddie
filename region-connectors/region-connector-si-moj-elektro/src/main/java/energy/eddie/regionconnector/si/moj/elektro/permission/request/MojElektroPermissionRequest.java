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
    private String permissionId;

    private String connectionId;

    @Column(length = 36)
    private String dataNeedId;

    @Enumerated(EnumType.STRING)
    private PermissionProcessStatus status;

    private Granularity granularity;

    private LocalDate permissionStart;

    private LocalDate permissionEnd;

    private ZonedDateTime eventCreated;

    @Nullable
    private LocalDate latestMeterReadingEndDate;

    private String apiToken;

    @Nullable
    private String meteringPoint;

    public MojElektroPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            Granularity granularity,
            LocalDate permissionStart,
            LocalDate permissionEnd,
            ZonedDateTime eventCreated,
            @Nullable LocalDate latestMeterReadingEndDate,
            @Nullable String apiToken,
            @Nullable String meteringPoint
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.granularity = granularity;
        this.permissionStart = permissionStart;
        this.permissionEnd = permissionEnd;
        this.eventCreated = eventCreated;
        this.latestMeterReadingEndDate = latestMeterReadingEndDate;
        this.apiToken = apiToken;
        this.meteringPoint = meteringPoint;
    }

    protected MojElektroPermissionRequest() { }

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
        return eventCreated;
    }

    @Override
    public LocalDate start() {
        return permissionStart;
    }

    @Override
    public LocalDate end() {
        return permissionEnd;
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
