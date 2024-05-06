package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.persistence.OAuthTokenRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This handler terminates all permissions that are not needed anymore. This includes permission request that were
 * terminated by the eligible party or permission requests, which were fulfilled.
 */
@Component
public class TerminationAndFulfillmentHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationAndFulfillmentHandler.class);
    private final OAuthTokenRepository oAuthTokenRepository;

    public TerminationAndFulfillmentHandler(
            EventBus eventBus,
            OAuthTokenRepository oAuthTokenRepository
    ) {
        this.oAuthTokenRepository = oAuthTokenRepository;
        eventBus.filteredFlux(PermissionProcessStatus.TERMINATED)
                .subscribe(this::accept);
        eventBus.filteredFlux(PermissionProcessStatus.FULFILLED)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        String permissionId = permissionEvent.permissionId();
        LOGGER.atInfo()
              .addArgument(permissionEvent::status)
              .addArgument(permissionId)
              .log("Received {} permission request {}, deleting token");
        // Usually the token should be revoked here, but mijn aansluting does not support revocation of access or refresh tokens.
        // See: https://www.acc.mijnenergiedata.nl/autorisatieregister/.well-known/openid-configuration
        oAuthTokenRepository.deleteById(permissionId);
        LOGGER.info("Deleted token for permission request {}", permissionId);
    }
}
