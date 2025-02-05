package energy.eddie.regionconnector.cds.permission.requests;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
public class CdsPermissionRequest implements PermissionRequest {
    @Id
    @Column(name = "permission_id")
    private final String permissionId;
    @Column(name = "connection_id")
    private final String connectionId;
    @Column(name = "data_need_id")
    private final String dataNeedId;
    @Column(name = "status", columnDefinition = "text")
    @Enumerated(EnumType.STRING)
    private final PermissionProcessStatus status;
    @Embedded
    private final CdsDataSourceInformation dataSourceInformation;
    @Column(name = "created")
    private final ZonedDateTime created;
    @Column(name = "data_start")
    private final LocalDate dataStart;
    @Column(name = "data_end")
    private final LocalDate dataEnd;


    @SuppressWarnings("java:S107")
    public CdsPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            PermissionProcessStatus status,
            String cdsServer,
            ZonedDateTime created,
            LocalDate dataStart,
            LocalDate dataEnd
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.created = created;
        this.dataStart = dataStart;
        this.dataEnd = dataEnd;
        dataSourceInformation = new CdsDataSourceInformation(cdsServer);
    }

    @SuppressWarnings("NullAway")
    protected CdsPermissionRequest() {
        permissionId = null;
        connectionId = null;
        dataNeedId = null;
        status = null;
        dataSourceInformation = null;
        created = null;
        dataStart = null;
        dataEnd = null;
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
        return dataStart;
    }

    @Override
    public LocalDate end() {
        return dataEnd;
    }
}
