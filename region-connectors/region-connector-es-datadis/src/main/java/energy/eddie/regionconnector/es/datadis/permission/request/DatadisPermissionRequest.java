package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.AllowedGranularity;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

@Entity
@Table(schema = "es_datadis", name = "datadis_permission_request")
@SuppressWarnings("NullAway")
public class DatadisPermissionRequest implements EsPermissionRequest {
    @Transient
    private final DatadisDataSourceInformation dataSourceInformation = new DatadisDataSourceInformation(this);
    @Id
    private final String permissionId;
    private final String connectionId;
    private final String nif;
    private final String meteringPointId;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;
    private final String dataNeedId;
    @Column(columnDefinition = "text")
    @Enumerated(EnumType.STRING)
    private final Granularity granularity;
    @Nullable
    @Column(columnDefinition = "text")
    @Enumerated(EnumType.STRING)
    private final DistributorCode distributorCode;
    @Nullable
    private final Integer pointType;
    @Column(name = "latest_meter_reading")
    @Nullable
    private final LocalDate latestMeterReadingEndDate;
    @Column(columnDefinition = "text")
    @Enumerated(EnumType.STRING)
    private final PermissionProcessStatus status;
    @Nullable
    private final String errorMessage;
    @Column
    private final boolean productionSupport;
    private final ZonedDateTime created;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text", name = "allowed_granularity")
    private AllowedGranularity allowedGranularity;

    // just for JPA
    @SuppressWarnings("NullAway.Init")
    protected DatadisPermissionRequest() {
        permissionId = null;
        connectionId = null;
        granularity = null;
        nif = null;
        meteringPointId = null;
        start = null;
        end = null;
        dataNeedId = null;
        distributorCode = null;
        pointType = null;
        latestMeterReadingEndDate = null;
        status = null;
        errorMessage = null;
        productionSupport = false;
        created = null;
        allowedGranularity = null;
    }

    @SuppressWarnings("java:S107")
    public DatadisPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            Granularity granularity,
            String nif,
            String meteringPointId,
            LocalDate start,
            LocalDate end,
            @Nullable DistributorCode distributorCode,
            @Nullable Integer pointType,
            @Nullable LocalDate latestMeterReadingEndDate,
            PermissionProcessStatus status,
            @Nullable String errorMessage,
            boolean productionSupport,
            ZonedDateTime created,
            AllowedGranularity allowedGranularity
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.nif = nif;
        this.meteringPointId = meteringPointId;
        this.start = start;
        this.end = end;
        this.distributorCode = distributorCode;
        this.pointType = pointType;
        this.latestMeterReadingEndDate = latestMeterReadingEndDate;
        this.status = status;
        this.granularity = granularity;
        this.errorMessage = errorMessage;
        this.productionSupport = productionSupport;
        this.created = created;
        this.allowedGranularity = allowedGranularity;
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
    public String nif() {
        return nif;
    }

    @Override
    public String meteringPointId() {
        return meteringPointId;
    }

    @Override
    public Optional<DistributorCode> distributorCode() {
        return Optional.ofNullable(distributorCode);
    }

    @Override
    public Optional<Integer> pointType() {
        return Optional.ofNullable(this.pointType);
    }

    @Override
    public boolean productionSupport() {
        return productionSupport;
    }


    @Override
    public MeasurementType measurementType() {
        return switch (granularity) {
            case null -> throw new IllegalStateException("Granularity is null");
            case PT15M -> MeasurementType.QUARTER_HOURLY;
            case PT1H -> MeasurementType.HOURLY;
            default -> throw new IllegalStateException("Unsupported granularity: " + granularity);
        };
    }

    @Override
    public AllowedGranularity allowedGranularity() {
        return allowedGranularity;
    }

    @Override
    @Nullable
    public String errorMessage() {
        return errorMessage;
    }

    @Override
    public Granularity granularity() {
        return granularity;
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return Optional.ofNullable(this.latestMeterReadingEndDate);
    }
}
