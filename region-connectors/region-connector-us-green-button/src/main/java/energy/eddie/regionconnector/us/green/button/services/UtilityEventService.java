package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.us.green.button.dtos.WebhookEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsUnfulfillableEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtilityEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilityEventService.class);
    private final Outbox outbox;
    private final UsPermissionRequestRepository repository;

    public UtilityEventService(Outbox outbox, UsPermissionRequestRepository repository) {
        this.outbox = outbox;
        this.repository = repository;
    }

    public void receiveEvents(List<WebhookEvent> events) throws PermissionNotFoundException {
        for (var event : events) {
            receiveEvent(event);
        }
    }

    private void receiveEvent(WebhookEvent event) throws PermissionNotFoundException {
        if (event.authorizationUid() == null) {
            LOGGER.info("Got event {} without authorization UID", event.type());
            return;
        }
        var res = repository.findByAuthUid(event.authorizationUid());
        if (res == null) {
            throw new PermissionNotFoundException("unknown");
        }
        LOGGER.info("Got webhook event '{}' for permission request {}", event.type(), res.permissionId());
        switch (event.type()) {
            case "authorization_expired":
                if (res.status() != PermissionProcessStatus.EXTERNALLY_TERMINATED)
                    outbox.commit(new UsUnfulfillableEvent(res.permissionId(), false));
                break;
            case "authorization_revoked":
                outbox.commit(new UsSimpleEvent(res.permissionId(), PermissionProcessStatus.REVOKED));
                break;
            default:
                // No-Op
        }
    }
}
