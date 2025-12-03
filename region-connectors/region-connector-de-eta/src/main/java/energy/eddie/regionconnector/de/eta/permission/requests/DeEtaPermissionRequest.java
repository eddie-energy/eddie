package energy.eddie.regionconnector.de.eta.permission.requests;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "permission_request", schema = "de_eta")
public class DeEtaPermissionRequest implements PermissionRequest {
    @Id
    @Column(name = "permission_id")
    private final String permissionId;

    @Column(name = "connection_id")
    private final String connectionId;

    @Column(name = "data_need_id")
    private final String dataNeedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "text")
    private final PermissionProcessStatus status;

    @Embedded
    private final DeEtaDataSourceInformation dataSourceInformation;

    @Column(name = "created")
    private final ZonedDateTime created;

    @Embedded
    @AttributeOverride(name = "start", column = @Column(name = "data_start"))
    @AttributeOverride(name = "end", column = @Column(name = "data_end"))
    private final DateRange dataRange;

    @Column(name = "granularity")
    private final String granularity;

    @Column(name = "latest_reading")
    @Nullable
    private ZonedDateTime latestReading;

    public DeEtaPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            ZonedDateTime created,
            DateRange dataRange,
            String granularity
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.created = created;
        this.dataRange = dataRange;
        this.granularity = granularity;
        this.dataSourceInformation = new DeEtaDataSourceInformation();
    }

    @SuppressWarnings("NullAway")
    protected DeEtaPermissionRequest() {
        this.permissionId = null;
        this.connectionId = null;
        this.dataNeedId = null;
        this.status = null;
        this.created = null;
        this.dataRange = null;
        this.granularity = null;
        this.dataSourceInformation = null;
    }

    @Override
    public String permissionId() {return permissionId;}

    @Override
    public String connectionId() {return connectionId;}

    @Override
    public String dataNeedId() {return dataNeedId;}

    @Override
    public PermissionProcessStatus status() {return status;}

    @Override
    public DeEtaDataSourceInformation dataSourceInformation() {return dataSourceInformation;}

    @Override
    public ZonedDateTime created() {return created;}

    @Override
    public LocalDate start() {return dataRange != null ? dataRange.start() : null;}

    @Override
    public LocalDate end() {return dataRange != null ? dataRange.end() : null;}

    public String granularity() {return granularity;}
}
