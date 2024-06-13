package energy.eddie.regionconnector.us.green.button.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

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
    }

    public GreenButtonPermissionRequest(
            String permissionId,
            String connectionId,
            LocalDate start,
            LocalDate end,
            String dataNeedId,
            PermissionProcessStatus status,
            Granularity granularity,
            ZonedDateTime created
    ) {
        this(permissionId, connectionId, dataNeedId, start, end, granularity, status, created, null, null, null, null);
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
            String scope
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
}
