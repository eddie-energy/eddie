package energy.eddie.regionconnector.fr.enedis.permission.request.models;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisDataSourceInformation;
import jakarta.persistence.*;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "FUTURE_DATA_PERMISSIONS")
public class FutureDataPermission implements TimeframedPermissionRequest {
    @Transient
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Paris");
    @Transient
    private final DataSourceInformation dataSourceInformation = new EnedisDataSourceInformation();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "connection_id")
    private String connectionId;
    @Column(name = "permission_id")
    private String permissionId;
    @Column(name = "data_need_id")
    private String dataNeedId;
    @Column(name = "metering_point_id")
    private String meteringPointId;
    @Column(name = "valid_from")
    private Instant validFrom;
    @Column(name = "valid_to")
    private Instant validTo;
    @Column(name = "last_poll")
    private Instant lastPoll;
    private String state;
    @Transient
    private PermissionRequestState permissionRequestState;
    @Transient
    private ZonedDateTime createdAt;

    public FutureDataPermission() {
        this.createdAt = ZonedDateTime.now(ZONE_ID);
    }

    public FutureDataPermission(FutureDataPermission futureDataPermission) {
        this.id = futureDataPermission.id;
        this.connectionId = futureDataPermission.connectionId;
        this.permissionId = futureDataPermission.permissionId();
        this.dataNeedId = futureDataPermission.dataNeedId();
        this.meteringPointId = futureDataPermission.meteringPointId;
        this.validFrom = futureDataPermission.validFrom;
        this.validTo = futureDataPermission.validTo;
        this.lastPoll = futureDataPermission.lastPoll;
        this.state = futureDataPermission.state;
        this.permissionRequestState = futureDataPermission.permissionRequestState;
        this.createdAt = ZonedDateTime.now(ZONE_ID);
    }

    public FutureDataPermission withConnectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public FutureDataPermission withPermissionId(String permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public FutureDataPermission withDataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
        return this;
    }

    public FutureDataPermission withMeteringPointId(String meteringPointId) {
        this.meteringPointId = meteringPointId;
        return this;
    }

    public String getMeteringPointId() {
        return this.meteringPointId;
    }

    public FutureDataPermission withState(PermissionRequestState state) {
        this.state = state.status().toString();
        this.permissionRequestState = state;
        return this;
    }

    public FutureDataPermission withValidFrom(ZonedDateTime startDate) {
        this.validFrom = startDate.toInstant();
        return this;
    }

    public FutureDataPermission withValidTo(@Nullable ZonedDateTime endDate) {
        this.validTo = endDate != null ? endDate.toInstant() : null;
        return this;
    }

    public void setLastPoll(ZonedDateTime lastPoll) {
        this.lastPoll = lastPoll.toInstant();
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
        return permissionRequestState;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
    }

    @Override
    public ZonedDateTime created() {
        return createdAt;
    }

    @Override
    public void changeState(PermissionRequestState state) {
        this.permissionRequestState = state;
        this.state = state.status().toString();
    }

    @Override
    public ZonedDateTime start() {
        return ZonedDateTime.ofInstant(validFrom, ZONE_ID);
    }

    @Override
    public ZonedDateTime end() {
        return ZonedDateTime.ofInstant(validTo, ZONE_ID);
    }
}
