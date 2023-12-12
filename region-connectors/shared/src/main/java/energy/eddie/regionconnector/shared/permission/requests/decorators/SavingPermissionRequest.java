package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.StateTransitionException;

/**
 * This class will save a permission request everytime a potential state change has happened.
 */
public class SavingPermissionRequest<T extends PermissionRequest> implements PermissionRequest {
    protected final T permissionRequest;
    private final PermissionRequestRepository<T> permissionRequestRepository;

    public SavingPermissionRequest(T permissionRequest, PermissionRequestRepository<T> permissionRequestRepository) {
        this.permissionRequest = permissionRequest;
        this.permissionRequestRepository = permissionRequestRepository;
        permissionRequestRepository.save(permissionRequest);
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
    public void validate() throws StateTransitionException {
        executeAndSave(permissionRequest::validate);
    }

    @Override
    public void sendToPermissionAdministrator() throws StateTransitionException {
        executeAndSave(permissionRequest::sendToPermissionAdministrator);
    }

    @Override
    public void receivedPermissionAdministratorResponse() throws StateTransitionException {
        executeAndSave(permissionRequest::receivedPermissionAdministratorResponse);
    }

    @Override
    public void terminate() throws StateTransitionException {
        executeAndSave(permissionRequest::terminate);
    }

    @Override
    public void accept() throws StateTransitionException {
        executeAndSave(permissionRequest::accept);
    }

    @Override
    public void invalid() throws StateTransitionException {
        executeAndSave(permissionRequest::invalid);
    }

    @Override
    public void reject() throws StateTransitionException {
        executeAndSave(permissionRequest::reject);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PermissionRequest) {
            return permissionRequest.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return permissionRequest.hashCode();
    }

    private void executeAndSave(Transition transition) throws StateTransitionException {
        transition.transit();
        permissionRequestRepository.save(permissionRequest);
    }

    @FunctionalInterface
    private interface Transition {
        void transit() throws StateTransitionException;
    }
}