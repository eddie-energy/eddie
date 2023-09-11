package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import reactor.core.publisher.Sinks;

public class PermissionRequestFactory {

    private final EdaAdapter edaAdapter;
    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages;
    private final AtPermissionRequestRepository permissionRequestRepository;

    public PermissionRequestFactory(EdaAdapter edaAdapter, Sinks.Many<ConnectionStatusMessage> permissionStateMessages, AtPermissionRequestRepository permissionRequestRepository) {
        this.edaAdapter = edaAdapter;
        this.permissionStateMessages = permissionStateMessages;
        this.permissionRequestRepository = permissionRequestRepository;
    }

    public AtPermissionRequest create(String connectionId, CCMORequest ccmoRequest) {
        AtPermissionRequest permissionRequest = new EdaPermissionRequest(connectionId, ccmoRequest, edaAdapter);
        PermissionRequest savingPermissionRequest = new energy.eddie.regionconnector.shared.permission.requests.decorators.SavingPermissionRequest<>(permissionRequest, permissionRequestRepository);
        PermissionRequest messagingPermissionRequest = new energy.eddie.regionconnector.shared.permission.requests.decorators.MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);
        return new EdaPermissionRequestAdapter(
                new EdaPermissionRequestAdapter(permissionRequest, messagingPermissionRequest),
                savingPermissionRequest
        );
    }
}
