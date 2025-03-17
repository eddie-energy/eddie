package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.AuthorizationService;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "region-connector.cds.par.enabled", havingValue = "true")
public class RetryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryService.class);
    private final Outbox outbox;
    private final CdsPermissionRequestRepository repository;
    private final AuthorizationService authorizationService;
    private final CdsServerRepository serverRepository;

    public RetryService(
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

    @Scheduled(cron = "${region-connector.cds.retry:0 0 * * * *}")
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
