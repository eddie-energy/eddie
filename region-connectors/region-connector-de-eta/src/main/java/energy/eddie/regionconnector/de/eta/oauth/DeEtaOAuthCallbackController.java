package energy.eddie.regionconnector.de.eta.oauth;

import energy.eddie.regionconnector.de.eta.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth callback endpoint for DE-ETA Authorization Code flow.
 * Handles exchanging code for tokens and emits domain events.
 */
@RestController
public class DeEtaOAuthCallbackController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeEtaOAuthCallbackController.class);

    public static final String PATH_CALLBACK = "/region-connectors/de-eta/callback";

    private final DeEtaOAuthTokenService tokenService;
    private final Outbox outbox;
    private final DeEtaOAuthStateStore stateStore;

    public DeEtaOAuthCallbackController(DeEtaOAuthTokenService tokenService, Outbox outbox, DeEtaOAuthStateStore stateStore) {
        this.tokenService = tokenService;
        this.outbox = outbox;
        this.stateStore = stateStore;
    }

    @GetMapping(PATH_CALLBACK)
    public ResponseEntity<String> callback(@RequestParam(name = "code", required = false) String code,
                                           @RequestParam(name = "state", required = false) String state) {
        if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            // Attempt to attach to permission if state exists
            stateStore.find(state == null ? "" : state).ifPresent(s ->
                    outbox.commit(new energy.eddie.regionconnector.de.eta.permission.events.InvalidEvent(
                            s.permissionId().toString(), "Missing code/state"))
            );
            LOGGER.info("OAuth callback missing parameters; statePresent={} ", StringUtils.hasText(state));
            Metrics.counter("oauth.token.exchange.failure", Tags.of("region", "de-eta", "reason", "missing_params")).increment();
            return ResponseEntity.badRequest().body("Authorization failed");
        }
        try {
            var result = tokenService.exchangeAuthorizationCode(code, state);
            String permissionId = result.permissionId();
            // Emit success event for the permission
            outbox.commit(new AcceptedEvent(permissionId));
            var tokenExpiresAt = result.token().getExpiresAt();
            LOGGER.info("OAuth callback success for permissionId={} connectionId={} tokenExpiresAt={}",
                    permissionId, result.token().getConnectionId(), tokenExpiresAt);
            // Prefer minimal response; a redirect to a static page could be configured later.
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            // Emit invalid event if state is known
            stateStore.find(state).ifPresent(s ->
                    outbox.commit(new energy.eddie.regionconnector.de.eta.permission.events.InvalidEvent(
                            s.permissionId().toString(), "OAuth token exchange failed"))
            );
            LOGGER.warn("OAuth callback handling failed: {}", e.getClass().getSimpleName());
            Metrics.counter("oauth.token.exchange.failure", Tags.of("region", "de-eta", "reason", "exception")).increment();
            return ResponseEntity.badRequest().body("Authorization failed");
        }
    }
}
