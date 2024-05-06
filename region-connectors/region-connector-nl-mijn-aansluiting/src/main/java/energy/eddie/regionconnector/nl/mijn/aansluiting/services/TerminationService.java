package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);
    private final Outbox outbox;
    private final NlPermissionRequestRepository permissionRequestRepository;

    public TerminationService(Outbox outbox, NlPermissionRequestRepository permissionRequestRepository) {
        this.outbox = outbox;
        this.permissionRequestRepository = permissionRequestRepository;
    }

    public void terminate(String permissionId) {
        LOGGER.info("Received termination for permission request {}", permissionId);
        if (!permissionRequestRepository.existsByPermissionIdAndStatus(permissionId,
                                                                       PermissionProcessStatus.ACCEPTED)) {
            LOGGER.info("Permission request {} does not exist or was not accepted yet", permissionId);
            return;
        }
        outbox.commit(new NlSimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
    }
}
