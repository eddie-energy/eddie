package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Adapter to use adapt the shared decorators to the EsPermissionRequest interface.
 */
public final class DatadisPermissionRequestAdapter implements EsPermissionRequest {

    private final EsPermissionRequest esPermissionRequest;
    private final PermissionRequest adaptee;

    public DatadisPermissionRequestAdapter(EsPermissionRequest esPermissionRequest, PermissionRequest adaptee) {
        this.esPermissionRequest = esPermissionRequest;
        this.adaptee = adaptee;
    }

    @Override
    public String permissionId() {
        return esPermissionRequest.permissionId();
    }

    @Override
    public String connectionId() {
        return esPermissionRequest.connectionId();
    }

    @Override
    public String dataNeedId() {
        return esPermissionRequest.dataNeedId();
    }

    @Override
    public PermissionRequestState state() {
        return esPermissionRequest.state();
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return esPermissionRequest.dataSourceInformation();
    }

    @Override
    public void changeState(PermissionRequestState state) {
        esPermissionRequest.changeState(state);
    }


    @Override
    public void validate() throws StateTransitionException {
        adaptee.validate();
    }

    @Override
    public void sendToPermissionAdministrator() throws StateTransitionException {
        adaptee.sendToPermissionAdministrator();
    }

    @Override
    public void receivedPermissionAdministratorResponse() throws StateTransitionException {
        adaptee.receivedPermissionAdministratorResponse();
    }

    @Override
    public void terminate() throws StateTransitionException {
        adaptee.terminate();
    }

    @Override
    public void accept() throws StateTransitionException {
        adaptee.accept();
    }

    @Override
    public void invalid() throws StateTransitionException {
        adaptee.invalid();
    }

    @Override
    public void reject() throws StateTransitionException {
        adaptee.reject();
    }

    @Override
    public String nif() {
        return esPermissionRequest.nif();
    }

    @Override
    public String meteringPointId() {
        return esPermissionRequest.meteringPointId();
    }

    @Override
    public Optional<DistributorCode> distributorCode() {
        return esPermissionRequest.distributorCode();
    }

    @Override
    public void setDistributorCode(DistributorCode distributorCode) {
        esPermissionRequest.setDistributorCode(distributorCode);
    }

    @Override
    public Optional<Integer> pointType() {
        return esPermissionRequest.pointType();
    }

    @Override
    public MeasurementType measurementType() {
        return esPermissionRequest.measurementType();
    }

    @Override
    public void setPointType(Integer pointType) {
        esPermissionRequest.setPointType(pointType);
    }

    @Override
    public ZonedDateTime permissionStart() {
        return esPermissionRequest.permissionStart();
    }

    @Override
    public ZonedDateTime permissionEnd() {
        return esPermissionRequest.permissionEnd();
    }

    @Override
    public ZonedDateTime requestDataFrom() {
        return esPermissionRequest.requestDataFrom();
    }

    @Override
    public ZonedDateTime requestDataTo() {
        return esPermissionRequest.requestDataTo();
    }

    @Override
    public Optional<ZonedDateTime> lastPulledMeterReading() {
        return esPermissionRequest.lastPulledMeterReading();
    }

    @Override
    public void setLastPulledMeterReading(ZonedDateTime lastPulledMeterReading) {
        esPermissionRequest.setLastPulledMeterReading(lastPulledMeterReading);
    }
}