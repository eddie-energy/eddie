package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

@Entity
@Table(schema = "fr_enedis", name = "enedis_permission_request")
@SuppressWarnings("NullAway")
public class EnedisPermissionRequest implements FrEnedisPermissionRequest {
    private static final EnedisDataSourceInformation dataSourceInformation = new EnedisDataSourceInformation();
    @Id
    @Column(name = "permission_id")
    private final String permissionId;
    @Column(name = "connection_id")
    private final String connectionId;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;
    @Column(name = "data_need_id")
    private final String dataNeedId;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private final PermissionProcessStatus status;
    @Column(name = "granularity")
    @Enumerated(EnumType.STRING)
    private final Granularity granularity;
    @Nullable
    @Column(name = "usage_point_id")
    private final String usagePointId;
    @Nullable
    @Column(name = "latest_meter_reading_end_date")
    private final LocalDate latestMeterReadingEndDate;

    @Column(name = "created")
    private final ZonedDateTime created;
    @Column(name = "usage_point_type")
    @Enumerated(EnumType.STRING)
    private final UsagePointType usagePointType;

    // just for JPA
    @SuppressWarnings("NullAway.Init")
    protected EnedisPermissionRequest() {
        permissionId = null;
        connectionId = null;
        start = null;
        end = null;
        dataNeedId = null;
        status = null;
        granularity = null;
        usagePointId = null;
        latestMeterReadingEndDate = null;
        created = null;
        usagePointType = null;
    }

    public EnedisPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            PermissionProcessStatus status
    ) {
        this(permissionId,
             connectionId,
             dataNeedId,
             start,
             end,
             granularity,
             status,
             null,
             null,
             ZonedDateTime.now(ZONE_ID_FR),
             UsagePointType.CONSUMPTION);
    }

    @SuppressWarnings("java:S107")
    public EnedisPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            PermissionProcessStatus status,
            @Nullable String usagePointId,
            @Nullable LocalDate latestMeterReadingEndDate,
            ZonedDateTime created,
            UsagePointType usagePointType
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.start = start;
        this.end = end;
        this.dataNeedId = dataNeedId;
        this.status = status;
        this.granularity = granularity;
        this.usagePointId = usagePointId;
        this.latestMeterReadingEndDate = latestMeterReadingEndDate;
        this.created = created;
        this.usagePointType = usagePointType;
    }

    @Override
    public String usagePointId() {
        return usagePointId;
    }


    @Override
    public Granularity granularity() {
        return granularity;
    }

    @Override
    public String customerIdentification() {
        return "";
    }

    @Override
    public String meteringPointEAN() {
        return "";
    }

    @Override
    public Optional<ZonedDateTime> latestMeterReading() {
        return Optional.empty();
    }

    @Override
    public UsagePointType usagePointType() {
        return usagePointType;
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
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return Optional.ofNullable(latestMeterReadingEndDate);
    }
}
