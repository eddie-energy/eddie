// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.services.utility.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.dtos.WebhookEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsAuthorizationUpdateFinishedEvent;
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
    private final MeterEventCallbacks meterEventCallbacks;

    public UtilityEventService(
            Outbox outbox,
            UsPermissionRequestRepository repository,
            MeterEventCallbacks meterEventCallbacks
    ) {
        this.outbox = outbox;
        this.repository = repository;
        this.meterEventCallbacks = meterEventCallbacks;
    }

    public void receiveEvents(List<WebhookEvent> events) {
        for (var event : events) {
            receiveEvent(event);
        }
    }

    private void receiveEvent(WebhookEvent event) {
        var authUid = event.authorizationUid();
        if (authUid == null) {
            LOGGER.info("Got event {} without authorization UID", event.type());
            return;
        }
        var res = repository.findByAuthUid(authUid);
        if (res == null) {
            LOGGER.info("Got unknown permission request with authorization UID {}", authUid);
            return;
        }
        var permissionId = res.permissionId();
        LOGGER.debug("Got webhook event '{}' for permission request {}", event.type(), permissionId);
        switch (event.type()) {
            case "authorization_expired":
                LOGGER.info("Got authorization expired event for {} permission request {}",
                            res.status(),
                            permissionId);
                if (res.status() != PermissionProcessStatus.EXTERNALLY_TERMINATED) {
                    outbox.commit(new UsUnfulfillableEvent(permissionId, false));
                }
                break;
            case "authorization_revoked":
                LOGGER.info("Got authorization revoked for {} permission request {}",
                            res.status(),
                            permissionId);
                outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
                break;
            case "meter_created":
                meterEventCallbacks.onMeterCreatedEvent(event, res);
                break;
            case "meter_historical_collection_finished_successful":
                meterEventCallbacks.onHistoricalCollectionFinishedEvent(event, res);
                break;
            case "authorization_update_finished_successful":
                if (res.status() == PermissionProcessStatus.ACCEPTED) {
                    outbox.commit(new UsAuthorizationUpdateFinishedEvent(permissionId));
                } else {
                    LOGGER.info("Permission request {} is not accepted, but {}, not emitting authorization event",
                                permissionId,
                                res.status());
                }
                break;
            default:
                // No-Op
        }
    }
}
