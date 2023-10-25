package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Adapter to use adapt the shared decorators to the AtPermissionRequest interface.
 */
public final class EdaPermissionRequestAdapter implements AtPermissionRequest {

    private final AtPermissionRequest edaPermissionRequest;
    private final PermissionRequest adaptee;

    public EdaPermissionRequestAdapter(AtPermissionRequest atPermissionRequest, PermissionRequest adaptee) {
        this.edaPermissionRequest = atPermissionRequest;
        this.adaptee = adaptee;
    }

    @Override
    public String permissionId() {
        return edaPermissionRequest.permissionId();
    }

    @Override
    public String connectionId() {
        return edaPermissionRequest.connectionId();
    }

    @Override
    public String dataNeedId() {
        return edaPermissionRequest.dataNeedId();
    }

    @Override
    public PermissionRequestState state() {
        return edaPermissionRequest.state();
    }

    @Override
    public void changeState(PermissionRequestState state) {
        edaPermissionRequest.changeState(state);
    }

    @Override
    public String cmRequestId() {
        return edaPermissionRequest.cmRequestId();
    }

    @Override
    public String conversationId() {
        return edaPermissionRequest.conversationId();
    }

    @Override
    public Optional<String> meteringPointId() {
        return edaPermissionRequest.meteringPointId();
    }

    @Override
    public void setMeteringPointId(String meteringPointId) {
        edaPermissionRequest.setMeteringPointId(meteringPointId);
    }

    @Override
    public LocalDate dataFrom() {
        return edaPermissionRequest.dataFrom();
    }

    @Override
    public Optional<LocalDate> dataTo() {
        return edaPermissionRequest.dataTo();
    }

    @Override
    public void validate() throws FutureStateException, PastStateException {
        adaptee.validate();
    }

    @Override
    public void sendToPermissionAdministrator() throws FutureStateException, PastStateException {
        adaptee.sendToPermissionAdministrator();
    }

    @Override
    public void receivedPermissionAdministratorResponse() throws FutureStateException, PastStateException {
        adaptee.receivedPermissionAdministratorResponse();
    }

    @Override
    public void terminate() throws FutureStateException, PastStateException {
        adaptee.terminate();
    }

    @Override
    public void accept() throws FutureStateException, PastStateException {
        adaptee.accept();
    }

    @Override
    public void invalid() throws FutureStateException, PastStateException {
        adaptee.invalid();
    }

    @Override
    public void rejected() throws FutureStateException, PastStateException {
        adaptee.rejected();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PermissionRequest) {
            return edaPermissionRequest.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return edaPermissionRequest.hashCode();
    }
}
