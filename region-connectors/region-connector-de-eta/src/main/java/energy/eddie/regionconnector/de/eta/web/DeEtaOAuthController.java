package energy.eddie.regionconnector.de.eta.web;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.oauth.DeEtaOAuthProperties;
import energy.eddie.regionconnector.de.eta.oauth.DeEtaOAuthStateStore;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/**
 * Starts the OAuth Authorization Code flow by redirecting the user to ETA+ authorize endpoint.
 */
@RestController
public class DeEtaOAuthController {
    public static final String PATH_AUTHORIZE = "/region-connectors/de-eta/authorize/{permissionId}";

    private static final Logger LOGGER = LoggerFactory.getLogger(DeEtaOAuthController.class);
    private final DeEtaPermissionRequestRepository permissionRepo;
    private final DeEtaOAuthProperties oauthProperties;
    private final DeEtaOAuthStateStore stateStore;

    public DeEtaOAuthController(
            DeEtaPermissionRequestRepository permissionRepo,
            DeEtaOAuthProperties oauthProperties,
            DeEtaOAuthStateStore stateStore
    ) {
        this.permissionRepo = permissionRepo;
        this.oauthProperties = oauthProperties;
        this.stateStore = stateStore;
    }

    @GetMapping(PATH_AUTHORIZE)
    public ResponseEntity<Void> authorize(@PathVariable UUID permissionId) throws PermissionNotFoundException {
        if (!oauthProperties.enabled()) {
            return ResponseEntity.notFound().build();
        }

        Optional<DeEtaPermissionRequest> opt = permissionRepo.findByPermissionId(permissionId.toString());
        var permission = opt.orElseThrow(() -> new PermissionNotFoundException(
                "Permission '%s' not found".formatted(permissionId)));

        // Validate state that can be authorized (only VALIDATED is acceptable for starting OAuth)
        if (permission.status() != PermissionProcessStatus.VALIDATED) {
            LOGGER.info("Authorize rejected for permissionId={} status={}", permissionId, permission.status());
            return ResponseEntity.status(409).build();
        }

        // Build a secure random state and persist mapping with TTL
        String state = UUID.randomUUID().toString();
        stateStore.save(permissionId, permission.connectionId(), state);

        URI redirect = UriComponentsBuilder.fromUriString(oauthProperties.authorizeUrl())
                .queryParam("client_id", oauthProperties.clientId())
                .queryParam("redirect_uri", oauthProperties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", oauthProperties.scope())
                .queryParam("state", state)
                .build(true) // encode the params
                .toUri();

        LOGGER.info("Authorize redirect created for permissionId={} connectionId={} to uri={}", permissionId,
                permission.connectionId(), redirect.getHost());
        return ResponseEntity.status(302)
                .location(redirect)
                .build();
    }
}
