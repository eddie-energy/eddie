package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

@Entity
@Table(schema = "dk_energinet", name = "energinet_permission_request")
@SuppressWarnings("NullAway")
public class EnerginetPermissionRequest implements DkEnerginetPermissionRequest {
    private static final EnerginetDataSourceInformation dataSourceInformation = new EnerginetDataSourceInformation();
    @Id
    @Column(length = 36)
    private final String permissionId;
    @Column(columnDefinition = "text")
    private final String connectionId;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;
    @Column(columnDefinition = "text")
    private final String meteringPoint;
    @Column(length = 36)
    private final String dataNeedId;
    @Enumerated(EnumType.STRING)
    private final Granularity granularity;
    @Column(name = "latest_meter_reading_end_date")
    @Nullable
    private final LocalDate latestMeterReadingEndDate;
    @Column(columnDefinition = "text")
    private final String refreshToken;
    @Nullable
    @Column(columnDefinition = "text")
    private final String accessToken;
    @Enumerated(EnumType.STRING)
    private final PermissionProcessStatus status;
    private final ZonedDateTime created;

    // just for JPA
    protected EnerginetPermissionRequest() {
        permissionId = null;
        connectionId = null;
        start = null;
        end = null;
        meteringPoint = null;
        dataNeedId = null;
        granularity = null;
        latestMeterReadingEndDate = null;
        refreshToken = null;
        accessToken = null;
        status = null;
        created = null;
    }

    // Too many arguments for sonar
    @SuppressWarnings("java:S107")
    public EnerginetPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String meteringPoint,
            String refreshToken,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            @Nullable String accessToken,
            PermissionProcessStatus status,
            ZonedDateTime created
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.accessToken = accessToken;
        this.meteringPoint = meteringPoint;
        this.dataNeedId = dataNeedId;
        this.latestMeterReadingEndDate = null;
        this.refreshToken = refreshToken;
        this.start = start;
        this.end = end;
        this.granularity = granularity;
        this.status = status;
        this.created = created;
    }

    @Override
    public String refreshToken() {
        return refreshToken;
    }

    @Override
    @Nullable
    public String accessToken() {
        return accessToken;
    }

    @Override
    public Granularity granularity() {
        return granularity;
    }

    @Override
    public String meteringPoint() {
        return meteringPoint;
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
    public PermissionRequestState state() {
        return null;
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
    public void changeState(PermissionRequestState state) {
        throw new IllegalStateException("Not used anymore");
    }

    @Override
    public LocalDate start() {
        return start;
    }

    @Override
    public LocalDate end() {
        return end;
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return Optional.ofNullable(latestMeterReadingEndDate);
    }

    @Override
    public void updateLatestMeterReadingEndDate(LocalDate date) {
        throw new IllegalStateException("Not used anymore");
    }
}
