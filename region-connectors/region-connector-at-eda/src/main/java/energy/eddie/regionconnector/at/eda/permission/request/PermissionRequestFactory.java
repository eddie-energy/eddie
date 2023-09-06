package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
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
        return new MessagingPermissionRequest(
                new SavingPermissionRequest(
                        new EdaPermissionRequest(connectionId, ccmoRequest, edaAdapter),
                        permissionRequestRepository
                ),
                permissionStateMessages
        );
    }
}
