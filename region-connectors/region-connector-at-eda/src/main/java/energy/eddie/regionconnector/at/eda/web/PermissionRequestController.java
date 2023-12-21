package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestCreationService;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;

@RestController
public class PermissionRequestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final PermissionRequestService permissionRequestService;
    private final PermissionRequestCreationService creationService;

    public PermissionRequestController(PermissionRequestService permissionRequestService, PermissionRequestCreationService creationService) {
        this.permissionRequestService = permissionRequestService;
        this.creationService = creationService;
    }

    @GetMapping(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
    public ResponseEntity<ConnectionStatusMessage> permissionStatus(@PathVariable String permissionId) throws PermissionNotFoundException {
        var statusMessage = permissionRequestService.findConnectionStatusMessageById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        return ResponseEntity.ok(statusMessage);
    }

    @PostMapping(value = PATH_PERMISSION_REQUEST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedPermissionRequest createPermissionRequest(
            @ModelAttribute @Valid PermissionRequestForCreation permissionRequestForCreation
    ) throws StateTransitionException {
        LOGGER.info("Creating new permission request");
        return creationService.createAndSendPermissionRequest(permissionRequestForCreation);
    }
}