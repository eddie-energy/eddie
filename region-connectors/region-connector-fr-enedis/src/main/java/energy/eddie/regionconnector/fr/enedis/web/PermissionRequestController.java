package energy.eddie.regionconnector.fr.enedis.web;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.validation.Valid;
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
    private final PermissionRequestService permissionRequestService;

    public PermissionRequestController(PermissionRequestService permissionRequestService) {
        this.permissionRequestService = permissionRequestService;
    }

    @GetMapping(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
    public ConnectionStatusMessage permissionStatus(@PathVariable String permissionId) {
        Optional<ConnectionStatusMessage> connectionStatusMessage =
                permissionRequestService.findConnectionStatusMessageById(permissionId);
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
    ) throws StateTransitionException {
        CreatedPermissionRequest createdPermissionRequest = permissionRequestService.createPermissionRequest(permissionRequest);
        URI location = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
                .expand(createdPermissionRequest.permissionId());
        return ResponseEntity
                .created(location)
                .body(createdPermissionRequest);
    }

    @GetMapping(value = "/authorization-callback")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> callback(@RequestParam("state") String stateString, @RequestParam("usage_point_id") String usagePointId)
            throws StateTransitionException, PermissionNotFoundException {
        permissionRequestService.authorizePermissionRequest(stateString, usagePointId);
        return ResponseEntity.ok("Access Granted. You can close this tab now.");
    }
}