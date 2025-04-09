package energy.eddie.regionconnector.cds.tasks;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.AuthorizationService;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "region-connector.cds.par.enabled", havingValue = "true")
public class RetrySendingTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetrySendingTask.class);
    private final Outbox outbox;
    private final CdsPermissionRequestRepository repository;
    private final AuthorizationService authorizationService;
    private final CdsServerRepository serverRepository;

    public RetrySendingTask(
            Outbox outbox,
            CdsPermissionRequestRepository repository,
            AuthorizationService authorizationService,
            CdsServerRepository serverRepository
    ) {
        this.outbox = outbox;
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.serverRepository = serverRepository;
    }

    @RetrySending
    public void retry() {
        var permissionRequests = repository.findByStatus(PermissionProcessStatus.UNABLE_TO_SEND);
        for (var permissionRequest : permissionRequests) {
            var permissionId = permissionRequest.permissionId();
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.VALIDATED));
            var cdsServerId = permissionRequest.dataSourceInformation().cdsServerId();
            var cdsServer = serverRepository.getReferenceById(cdsServerId);
            LOGGER.info("Retrying sending permission request {} to {}", permissionId, cdsServer.name());
            authorizationService.createOAuthRequest(cdsServer, permissionId);
        }
    }
}
