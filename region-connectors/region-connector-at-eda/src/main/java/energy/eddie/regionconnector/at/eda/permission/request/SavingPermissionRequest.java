package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;

/**
 * This class will save a permission request everytime a potential state change has happened.
 */
public class SavingPermissionRequest implements AtPermissionRequest {
    private final AtPermissionRequest permissionRequest;
    private final AtPermissionRequestRepository permissionRequestRepository;

    public SavingPermissionRequest(AtPermissionRequest permissionRequest, AtPermissionRequestRepository permissionRequestRepository) {
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
    public String cmRequestId() {
        return permissionRequest.cmRequestId();
    }

    @Override
    public String conversationId() {
        return permissionRequest.conversationId();
    }

    @Override
    public PermissionRequestState state() {
        return permissionRequest.state();
    }

    @Override
    public void changeState(PermissionRequestState state) {
        permissionRequest.changeState(state);
    }

    @Override
    public void validate() throws FutureStateException, PastStateException {
        executeAndSave(permissionRequest::validate);
    }

    @Override
    public void sendToPermissionAdministrator() throws FutureStateException, PastStateException {
        executeAndSave(permissionRequest::sendToPermissionAdministrator);
    }

    @Override
    public void receivedPermissionAdministratorResponse() throws FutureStateException, PastStateException {
        executeAndSave(permissionRequest::receivedPermissionAdministratorResponse);
    }

    @Override
    public void terminate() throws FutureStateException, PastStateException {
        executeAndSave(permissionRequest::terminate);
    }

    @Override
    public void accept() throws FutureStateException, PastStateException {
        executeAndSave(permissionRequest::accept);
    }

    @Override
    public void invalid() throws FutureStateException, PastStateException {
        executeAndSave(permissionRequest::invalid);
    }

    @Override
    public void rejected() throws FutureStateException, PastStateException {
        executeAndSave(permissionRequest::rejected);
    }

    private void executeAndSave(Transition transition) throws FutureStateException, PastStateException {
        transition.transit();
        permissionRequestRepository.save(permissionRequest);
    }

    @FunctionalInterface
    private interface Transition {
        void transit() throws PastStateException, FutureStateException;
    }
}
