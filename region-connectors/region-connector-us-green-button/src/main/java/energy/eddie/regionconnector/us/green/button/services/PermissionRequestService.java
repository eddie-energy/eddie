package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.permission.events.UsUnfulfillableEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class PermissionRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private final Outbox outbox;
    private final UsPermissionRequestRepository repository;

    public PermissionRequestService(Outbox outbox, UsPermissionRequestRepository repository) {
        this.outbox = outbox;
        this.repository = repository;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void removeUnfulfillablePermissionRequests(Collection<String> permissionIds) {
        var permissionRequests = repository.findAllById(permissionIds);
        for (var permissionRequest : permissionRequests) {
            if (permissionRequest.allowedMeters().isEmpty()) {
                var permissionId = permissionRequest.permissionId();
                LOGGER.info("Marking permission request {} as unfulfillable, since no meter supports data need",
                            permissionId);
                outbox.commit(new UsUnfulfillableEvent(permissionId, true));
            }
        }
    }

    public List<UsGreenButtonPermissionRequest> findActivePermissionRequests() {
        return List.copyOf(repository.findActivePermissionRequests());
    }
}
