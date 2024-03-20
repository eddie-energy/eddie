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
import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static java.util.Objects.requireNonNull;

@Entity
@Table(schema = "es_datadis")
public class DatadisPermissionRequest extends TimestampedPermissionRequest implements EsPermissionRequest {
    @Transient
    private final DatadisDataSourceInformation dataSourceInformation = new DatadisDataSourceInformation(this);
    @Id
    private String permissionId;
    private String connectionId;
    private String nif;
    private String meteringPointId;
    private ZonedDateTime permissionStart;
    private ZonedDateTime permissionEnd;
    private ZonedDateTime requestDataFrom;
    private ZonedDateTime requestDataTo;
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
    @Nullable
    private ZonedDateTime lastPulledMeterReading;
    @Enumerated(EnumType.STRING)
    private PermissionProcessStatus status;
    @Nullable
    private String errorMessage;

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
        this.requestDataFrom = start.atStartOfDay(ZONE_ID_SPAIN);
        this.requestDataTo = end.atStartOfDay(ZONE_ID_SPAIN);

        this.permissionStart = ZonedDateTime.now(ZONE_ID_SPAIN);
        this.permissionEnd = latest(permissionStart, requestDataTo);
        this.state = factory.create(this, PermissionProcessStatus.CREATED).build();
        this.status = state.status();
    }

    @Override
    public DatadisPermissionRequest withStateBuilderFactory(StateBuilderFactory factory) {
        this.state = factory
                .create(this, status)
                .build();
        return this;
    }

    /**
     * Calculate the date furthest in the future.
     */
    private ZonedDateTime latest(ZonedDateTime first, ZonedDateTime second) {
        if (!first.isBefore(second)) {
            return first.plusDays(1); // if all the data is in the past we only need access for 1 day
        }

        return second;
    }

    @Override
    public void setDistributorCodeAndPointType(DistributorCode distributorCode, Integer pointType) {
        this.distributorCode = distributorCode;
        this.pointType = pointType;
    }

    @Override
    public Optional<Integer> pointType() {
        return Optional.ofNullable(this.pointType);
    }

    @Override
    public MeasurementType measurementType() {
        return this.measurementType;
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
    public ZonedDateTime permissionStart() {
        return permissionStart;
    }

    @Override
    public ZonedDateTime permissionEnd() {
        return permissionEnd;
    }

    @Override
    public ZonedDateTime start() {
        return requestDataFrom;
    }

    @Override
    public ZonedDateTime end() {
        return requestDataTo;
    }

    @Override
    public Optional<ZonedDateTime> lastPulledMeterReading() {
        return Optional.ofNullable(this.lastPulledMeterReading);
    }

    @Override
    public PermissionProcessStatus status() {
        return status;
    }

    @Override
    public void setLastPulledMeterReading(ZonedDateTime lastPulledMeterReading) {
        this.lastPulledMeterReading = lastPulledMeterReading;
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
}
