package energy.eddie.regionconnector.us.green.button.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import energy.eddie.regionconnector.us.green.button.permission.GreenButtonDataSourceInformation;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(schema = "us_green_button", name = "permission_request")
@SuppressWarnings("NullAway")
public class GreenButtonPermissionRequest implements UsGreenButtonPermissionRequest {
    @Embedded
    private final GreenButtonDataSourceInformation dataSourceInformation;
    @Id
    @Column(name = "permission_id")
    private final String permissionId;
    @Column(name = "connection_id")
    private final String connectionId;
    @Column(name = "data_need_id")
    private final String dataNeedId;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private final PermissionProcessStatus status;
    @Column(name = "granularity")
    @Enumerated(EnumType.STRING)
    @SuppressWarnings("unused")
    private final Granularity granularity;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;
    @Nullable
    @Column(name = "jump_off_url")
    private final String jumpOffUrl;
    @Nullable
    @Column(name = "scope")
    private final String scope;
    @Column(name = "created")
    private final ZonedDateTime created;
    @OneToMany(fetch = FetchType.EAGER, targetEntity = MeterReading.class)
    @JoinColumn(insertable = false, updatable = false, name = "permission_id", referencedColumnName = "permission_id")
    private final List<MeterReading> lastMeterReadings;
    @SuppressWarnings("unused")
    @Column(name = "auth_uid")
    private final String authUid;

    // just for JPA
    @SuppressWarnings("NullAway.Init")
    protected GreenButtonPermissionRequest() {
        dataSourceInformation = null;
        permissionId = null;
        connectionId = null;
        start = null;
        end = null;
        dataNeedId = null;
        status = null;
        granularity = null;
        scope = null;
        jumpOffUrl = null;
        created = null;
        lastMeterReadings = List.of();
        authUid = null;
    }

    @SuppressWarnings("java:S107")
    public GreenButtonPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            PermissionProcessStatus status,
            ZonedDateTime created,
            String countryCode,
            String companyId,
            String jumpOffUrl,
            String scope,
            String authUid
    ) {
        this(permissionId, connectionId, dataNeedId, start, end, granularity, status, created, countryCode,
             companyId, jumpOffUrl, scope, List.of(), authUid);
    }

    @SuppressWarnings({"java:S107"})
    public GreenButtonPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            PermissionProcessStatus status,
            ZonedDateTime created,
            String countryCode,
            String companyId,
            @Nullable String jumpOffUrl,
            @Nullable String scope,
            List<MeterReading> lastMeterReadings,
            String authUid
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.start = start;
        this.end = end;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.granularity = granularity;
        this.created = created;
        this.dataSourceInformation = new GreenButtonDataSourceInformation(companyId, countryCode);
        this.jumpOffUrl = jumpOffUrl;
        this.scope = scope;
        this.lastMeterReadings = lastMeterReadings;
        this.authUid = authUid;
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

    @Override
    public Optional<String> scope() {
        return Optional.ofNullable(scope);
    }

    @Override
    public Optional<String> jumpOffUrl() {
        return Optional.ofNullable(jumpOffUrl);
    }

    @Override
    public Optional<ZonedDateTime> latestMeterReadingEndDateTime() {
        return DateTimeUtils.oldestDateTime(MeterReading.lastMeterReadingDates(lastMeterReadings));
    }

    @Override
    public Set<String> allowedMeters() {
        return MeterReading.allowedMeters(lastMeterReadings);
    }

    @Override
    public List<MeterReading> lastMeterReadings() {
        return lastMeterReadings;
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return latestMeterReadingEndDateTime()
                .map(ZonedDateTime::toLocalDate);
    }

    @Override
    public String authorizationUid() {
        return authUid;
    }
}
