package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

public class DkEnerginetCustomerPermissionRequestAdapter implements DkEnerginetCustomerPermissionRequest {
    private final DkEnerginetCustomerPermissionRequest permissionRequest;
    private final PermissionRequest adaptee;

    public DkEnerginetCustomerPermissionRequestAdapter(DkEnerginetCustomerPermissionRequest permissionRequest, PermissionRequest adaptee) {
        this.permissionRequest = permissionRequest;
        this.adaptee = adaptee;
    }

    @Override
    public String permissionId() {
        return permissionRequest.permissionId();
    }

    @Override
    public String connectionId() {
        return permissionRequest.connectionId();
    }

    @Override
    public String dataNeedId() {
        return permissionRequest.dataNeedId();
    }

    @Override
    public PermissionRequestState state() {
        return permissionRequest.state();
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return permissionRequest.dataSourceInformation();
    }

    @Override
    public void changeState(PermissionRequestState state) {
        permissionRequest.changeState(state);
    }

    @Override
    public ZonedDateTime start() {
        return permissionRequest.start();
    }

    @Override
    public ZonedDateTime end() {
        return permissionRequest.end();
    }

    @Override
    public Mono<String> accessToken() {
        return permissionRequest.accessToken();
    }

    @Override
    public Granularity granularity() {
        return permissionRequest.granularity();
    }

    @Override
    public String meteringPoint() {
        return permissionRequest.meteringPoint();
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
}