package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.RegionalInformation;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.state.CreatedState;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static java.util.Objects.requireNonNull;


public class DatadisPermissionRequest implements EsPermissionRequest {
    private final String connectionId;
    private final String permissionId;
    private final String nif;
    private final String meteringPointId;
    private final ZonedDateTime permissionStart;
    private final ZonedDateTime permissionEnd;
    private final ZonedDateTime requestDataFrom;
    private final ZonedDateTime requestDataTo;
    private final MeasurementType measurementType;
    private final String dataNeedId;
    private final DatadisRegionalInformation regionalInformation = new DatadisRegionalInformation();
    private PermissionRequestState state;
    @Nullable
    private String distributorCode;
    @Nullable
    private Integer pointType;
    @Nullable
    private ZonedDateTime lastPulledMeterReading;

    public DatadisPermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String nif,
            String meteringPointId,
            MeasurementType measurementType,
            ZonedDateTime requestDataFrom,
            ZonedDateTime requestDataTo,
            AuthorizationApi authorizationApi,
            AuthorizationResponseHandler authorizationResponseHandler) {
        requireNonNull(permissionId);
        requireNonNull(connectionId);
        requireNonNull(dataNeedId);
        requireNonNull(nif);
        requireNonNull(meteringPointId);
        requireNonNull(measurementType);
        requireNonNull(requestDataFrom);
        requireNonNull(requestDataTo);
        requireNonNull(authorizationApi);
        requireNonNull(authorizationResponseHandler);

        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.nif = nif;
        this.meteringPointId = meteringPointId;
        this.measurementType = measurementType;
        this.requestDataFrom = requestDataFrom;
        this.requestDataTo = requestDataTo;

        this.permissionStart = ZonedDateTime.now(ZONE_ID_SPAIN);
        this.permissionEnd = latest(permissionStart, requestDataTo);
        this.state = new CreatedState(this, authorizationApi, authorizationResponseHandler);
    }

    /**
     * Calculate the date furthest in the future.
     */
    private ZonedDateTime latest(ZonedDateTime first, ZonedDateTime second) {
        if (second == null) {
            return first;
        }

        if (first.isAfter(second)) {
            return first; // if all the data is in the past we only need access for 1 day
        }

        return second;
    }

    @Override
    public void setDistributorCode(String distributorCode) {
        this.distributorCode = distributorCode;
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
    public void setPointType(Integer pointType) {
        this.pointType = pointType;
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
    public RegionalInformation regionalInformation() {
        return regionalInformation;
    }


    @Override
    public void changeState(PermissionRequestState state) {
        this.state = state;
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
    public Optional<String> distributorCode() {
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
    public ZonedDateTime requestDataFrom() {
        return requestDataFrom;
    }

    @Override
    public ZonedDateTime requestDataTo() {
        return requestDataTo;
    }

    @Override
    public Optional<ZonedDateTime> lastPulledMeterReading() {
        return Optional.ofNullable(this.lastPulledMeterReading);
    }

    @Override
    public void setLastPulledMeterReading(ZonedDateTime lastPulledMeterReading) {
        this.lastPulledMeterReading = lastPulledMeterReading;
    }
}