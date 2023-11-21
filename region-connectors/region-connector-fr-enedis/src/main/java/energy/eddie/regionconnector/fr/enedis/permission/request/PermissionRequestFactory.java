package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.shared.permission.requests.decorators.MessagingPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.decorators.SavingPermissionRequest;
import io.javalin.http.Context;
import reactor.core.publisher.Sinks;

public class PermissionRequestFactory {
    private final PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository;
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;
    private final EnedisConfiguration configuration;

    public PermissionRequestFactory(PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository,
                                    Sinks.Many<ConnectionStatusMessage> connectionStatusSink,
                                    EnedisConfiguration configuration) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.connectionStatusSink = connectionStatusSink;
        this.configuration = configuration;
    }

    public TimeframedPermissionRequest create(Context ctx) {
        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest(ctx, configuration);
        PermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, connectionStatusSink);
        PermissionRequest savingPermissionRequest = new SavingPermissionRequest<>(
                new TimeFramedPermissionRequestAdapter(permissionRequest, messagingPermissionRequest),
                permissionRequestRepository
        );
        return new TimeFramedPermissionRequestAdapter(
                permissionRequest,
                savingPermissionRequest
        );
    }
}