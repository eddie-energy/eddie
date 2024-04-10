package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.TimestampedPermissionRequest;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static java.util.Objects.requireNonNull;

@Entity
@Table(schema = "es_datadis", name = "datadis_permission_request")
public class DatadisPermissionRequest extends TimestampedPermissionRequest implements EsPermissionRequest {
    @Transient
    private final DatadisDataSourceInformation dataSourceInformation = new DatadisDataSourceInformation(this);
    @Id
    private String permissionId;
    private String connectionId;
    private String nif;
    private String meteringPointId;
    private LocalDate requestDataFrom;
    private LocalDate requestDataTo;
    @Enumerated(EnumType.STRING)
    private MeasurementType measurementType;
    private String dataNeedId;
    @Transient
    private PermissionRequestState state;
    @Nullable
    @Enumerated(EnumType.STRING)
    private DistributorCode distributorCode;
    @Nullable
    private Integer pointType;
    @Column(name = "latest_meter_reading_end_date")
    @Nullable
    private LocalDate latestMeterReadingEndDate;
    @Enumerated(EnumType.STRING)
    private PermissionProcessStatus status;
    @Nullable
    private String errorMessage;
    private boolean productionSupport;

    // just for JPA
    @SuppressWarnings("NullAway.Init")
    protected DatadisPermissionRequest() {
        super(ZONE_ID_SPAIN);
    }

    public DatadisPermissionRequest(
            String permissionId,
            PermissionRequestForCreation requestForCreation,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            StateBuilderFactory factory
    ) {
        super(ZONE_ID_SPAIN);
        requireNonNull(permissionId);
        requireNonNull(requestForCreation);
        requireNonNull(factory);

        this.permissionId = permissionId;
        this.connectionId = requestForCreation.connectionId();
        this.dataNeedId = requestForCreation.dataNeedId();
        this.nif = requestForCreation.nif();
        this.meteringPointId = requestForCreation.meteringPointId();
        this.measurementType = switch (granularity) {
            case PT15M -> MeasurementType.QUARTER_HOURLY;
            case PT1H -> MeasurementType.HOURLY;
            default -> throw new IllegalArgumentException("Unsupported granularity: " + granularity);
        };
        this.requestDataFrom = start;
        this.requestDataTo = end;

        this.state = factory.create(this, PermissionProcessStatus.CREATED).build();
        this.status = state.status();
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
    public void validate() throws StateTransitionException {
        state.validate();
    }

    @Override
    public void sendToPermissionAdministrator() throws StateTransitionException {
        state.sendToPermissionAdministrator();
    }

    @Override
    public void receivedPermissionAdministratorResponse() throws StateTransitionException {
        state.receivedPermissionAdministratorResponse();
    }

    @Override
    public void terminate() throws StateTransitionException {
        state.terminate();
    }

    @Override
    public void accept() throws StateTransitionException {
        state.accept();
    }

    @Override
    public void invalid() throws StateTransitionException {
        state.invalid();
    }

    @Override
    public void reject() throws StateTransitionException {
        state.reject();
    }

    @Override
    public LocalDate start() {
        return requestDataFrom;
    }

    @Override
    public LocalDate end() {
        return requestDataTo;
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
    public DatadisPermissionRequest withStateBuilderFactory(StateBuilderFactory factory) {
        this.state = factory
                .create(this, status)
                .build();
        return this;
    }

    @Override
    public void setDistributorCodeAndPointType(DistributorCode distributorCode, Integer pointType) {
        this.distributorCode = distributorCode;
        this.pointType = pointType;
    }

    @Override
    public MeasurementType measurementType() {
        return this.measurementType;
    }

    @Override
    @Nullable
    public String errorMessage() {
        return errorMessage;
    }

    @Override
    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public Optional<LocalDate> latestMeterReadingEndDate() {
        return Optional.ofNullable(this.latestMeterReadingEndDate);
    }

    @Override
    public void updateLatestMeterReadingEndDate(LocalDate date) {
        this.latestMeterReadingEndDate = date;
    }
}
