package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnector.ZONE_ID_FR;

@Entity
@Table(schema = "fr_enedis", name = "enedis_permission_request")
public class EnedisPermissionRequest implements FrEnedisPermissionRequest {
    private static final EnedisDataSourceInformation dataSourceInformation = new EnedisDataSourceInformation();
    @Id
    @Column(name = "permission_id")
    private String permissionId;
    @Column(name = "connection_id")
    private String connectionId;
    @Column(name = "start_timestamp")
    private ZonedDateTime start;
    @Column(name = "end_timestamp")
    private ZonedDateTime end;
    @Column(name = "data_need_id")
    private String dataNeedId;
    @Transient
    private PermissionRequestState state;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PermissionProcessStatus status;
    @Column(name = "granularity")
    @Enumerated(EnumType.STRING)
    private Granularity granularity;
    @Nullable
    @Column(name = "usage_point_id")
    private String usagePointId;
    @Nullable
    @Column(name = "latest_meter_reading")
    private LocalDate latestMeterReading;

    @Column(name = "created")
    private ZonedDateTime created;

    // just for JPA
    @SuppressWarnings("NullAway.Init")
    protected EnedisPermissionRequest() {
    }

    public EnedisPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            ZonedDateTime start,
            ZonedDateTime end,
            Granularity granularity,
            StateBuilderFactory factory
    ) {
        this.created = ZonedDateTime.now(ZONE_ID_FR);
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.state = factory.create(this, PermissionProcessStatus.CREATED).build();
        this.dataNeedId = dataNeedId;
        this.start = start.withZoneSameInstant(ZONE_ID_FR);
        this.end = end.withZoneSameInstant(ZONE_ID_FR);
        this.status = state.status();
        this.granularity = granularity;
    }

    public EnedisPermissionRequest(
            String connectionId,
            String dataNeedId,
            ZonedDateTime start,
            ZonedDateTime end,
            Granularity granularity,
            StateBuilderFactory factory
    ) {
        this(UUID.randomUUID().toString(), connectionId, dataNeedId, start, end, granularity, factory);
    }

    @Override
    public FrEnedisPermissionRequest withStateBuilderFactory(StateBuilderFactory factory) {
        this.state = factory
                .create(this, status)
                .build();
        return this;
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
    public PermissionRequestState state() {
        return state;
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
        this.state = state;
        this.status = state.status();
    }

    @Override
    public ZonedDateTime start() {
        return start;
    }

    @Override
    public ZonedDateTime end() {
        return end;
    }

    @Override
    public PermissionProcessStatus status() {
        return status;
    }

    @Override
    public Optional<String> usagePointId() {
        return Optional.ofNullable(usagePointId);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void setUsagePointId(String usagePointId) {
        this.usagePointId = usagePointId;
    }

    @Override
    public Granularity granularity() {
        return granularity;
    }

    @Override
    public Optional<LocalDate> latestMeterReading() {
        return Optional.ofNullable(latestMeterReading);
    }

    @Override
    public void updateLatestMeterReading(LocalDate latestMeterReading) {
        this.latestMeterReading = latestMeterReading;
    }
}
