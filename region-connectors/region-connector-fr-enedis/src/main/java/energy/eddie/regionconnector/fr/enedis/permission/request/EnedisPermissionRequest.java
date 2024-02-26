package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisCreatedState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.permission.requests.TimestampedPermissionRequest;
import jakarta.persistence.*;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnector.ZONE_ID_FR;

@Entity
@Table(schema = "fr_enedis")
public class EnedisPermissionRequest extends TimestampedPermissionRequest implements FrEnedisPermissionRequest {
    private static final EnedisDataSourceInformation dataSourceInformation = new EnedisDataSourceInformation();
    @Id
    private String permissionId;
    private String connectionId;
    @Column(name = "start_timestamp")
    private ZonedDateTime start;
    @Column(name = "end_timestamp")
    private ZonedDateTime end;
    private String dataNeedId;
    @Transient
    private PermissionRequestState state;
    @Enumerated(EnumType.STRING)
    private PermissionProcessStatus status;
    @Enumerated(EnumType.STRING)
    private Granularity granularity;


    // just for JPA
    @SuppressWarnings("NullAway.Init")
    protected EnedisPermissionRequest() {
        super(ZONE_ID_FR);
    }
    @Nullable
    private String usagePointId;

    public EnedisPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            ZonedDateTime start,
            ZonedDateTime end,
            Granularity granularity,
            StateBuilderFactory factory
            ) {
        super(ZONE_ID_FR);
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.state = factory.create(this, PermissionProcessStatus.CREATED).build();
        this.dataNeedId = dataNeedId;
        this.start = start;
        this.end = end;
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

    public EnedisPermissionRequest withStateBuilderFactory(StateBuilderFactory factory) {
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
}