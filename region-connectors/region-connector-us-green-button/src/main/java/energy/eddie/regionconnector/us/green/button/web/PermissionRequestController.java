package energy.eddie.regionconnector.us.green.button.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import energy.eddie.regionconnector.us.green.button.oauth.OAuthCallback;
import energy.eddie.regionconnector.us.green.button.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestAuthorizationService;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestCreationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriTemplate;

import java.net.URI;
import java.util.Optional;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;

@RestController
public class PermissionRequestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final PermissionRequestCreationService permissionRequestCreationService;
    private final PermissionRequestAuthorizationService permissionRequestAuthorizationService;

    public PermissionRequestController(
            PermissionRequestCreationService permissionRequestCreationService,
            PermissionRequestAuthorizationService permissionRequestAuthorizationService
    ) {
        this.permissionRequestCreationService = permissionRequestCreationService;
        this.permissionRequestAuthorizationService = permissionRequestAuthorizationService;
    }

    @GetMapping(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
    public ConnectionStatusMessage permissionStatus(@PathVariable String permissionId) {
        Optional<ConnectionStatusMessage> connectionStatusMessage =
                permissionRequestCreationService.findConnectionStatusMessageById(permissionId);
        if (connectionStatusMessage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find permission request");
        }
        return connectionStatusMessage.get();
    }

    @PostMapping(
            value = PATH_PERMISSION_REQUEST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(
            @RequestBody
            @Valid
            PermissionRequestForCreation permissionRequest
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, MissingClientIdException, MissingClientSecretException {
        var createdPermissionRequest = permissionRequestCreationService.createPermissionRequest(
                permissionRequest
        );
        URI location = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
                .expand(createdPermissionRequest.permissionId());
        return ResponseEntity
                .created(location)
                .body(createdPermissionRequest);
    }

    @GetMapping("/authorization-callback")
    public ResponseEntity<String> authorizationCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "state") String state
    ) throws PermissionNotFoundException, MissingClientIdException, MissingClientSecretException {
        LOGGER.info("Authorization callback received with code: {}, error: {}, state: {}", code, error, state);
        var oauthCallback = new OAuthCallback(Optional.ofNullable(code), Optional.ofNullable(error), state);
        permissionRequestAuthorizationService.authorizePermissionRequest(oauthCallback);

        return ResponseEntity.ok("You can close this tab now.");
    }
}
