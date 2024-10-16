package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);
    private final BePermissionRequestRepository repository;
    private final Outbox outbox;

    public TerminationService(BePermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    public void terminate(String permissionId) {
        var pr = repository.findByPermissionId(permissionId);
        if (pr.isEmpty()) {
            LOGGER.warn("Couldn't find permission with id {}", permissionId);
            return;
        }
        var status = pr.get().status();
        if (status != PermissionProcessStatus.ACCEPTED) {
            LOGGER.warn("Permission request {} is not in the accepted status, but in the {} status",
                        permissionId,
                        status);
        } else {
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.TERMINATED));
        }
    }
}
