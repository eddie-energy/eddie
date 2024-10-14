package energy.eddie.regionconnector.be.fluvius.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "permission_request", schema = "be_fluvius")
@SuppressWarnings({"NullAway", "unused"})
public class FluviusPermissionRequest implements PermissionRequest {
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
    @Column(name = "created")
    private final ZonedDateTime created;
    @Column(name = "flow")
    @Enumerated(EnumType.STRING)
    private final Flow flow;
    @Column(name = "short_url_identifier")
    private final String shortUrlIdentifier;
    @Column(name = "ean_number")
    private final String eanNumber;

    @SuppressWarnings("java:S107")
    public FluviusPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            Granularity granularity,
            LocalDate start,
            LocalDate end,
            ZonedDateTime created,
            Flow flow
    ) {
        this(
                permissionId,
                connectionId,
                dataNeedId,
                status,
                granularity,
                start,
                end,
                created,
                flow,
                null,
                null
        );
    }

    @SuppressWarnings("java:S107")
    public FluviusPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            Granularity granularity,
            LocalDate start,
            LocalDate end,
            ZonedDateTime created,
            Flow flow,
            @Nullable
            String shortUrlIdentifier,
            @Nullable
            String eanNumber
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.granularity = granularity;
        this.start = start;
        this.end = end;
        this.created = created;
        this.flow = flow;
        this.shortUrlIdentifier = shortUrlIdentifier;
        this.eanNumber = eanNumber;
    }

    protected FluviusPermissionRequest() {
        permissionId = null;
        connectionId = null;
        dataNeedId = null;
        status = null;
        granularity = null;
        start = null;
        end = null;
        created = null;
        flow = null;
        shortUrlIdentifier = null;
        eanNumber = null;
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
        return new FluviusDataSourceInformation();
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

    public Flow flow() {
        return flow;
    }

    public Granularity granularity() {
        return granularity;
    }

    public String shortUrlIdentifier() {
        return shortUrlIdentifier;
    }

    public String eanNumber() {
        return eanNumber;
    }
}
