package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.services.ConnectionStatusService;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestCreationAndValidationService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;

@RestController
public class PermissionRequestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final ConnectionStatusService connectionStatusService;
    private final PermissionRequestCreationAndValidationService creationService;

    public PermissionRequestController(ConnectionStatusService connectionStatusService,
                                       PermissionRequestCreationAndValidationService creationService) {
        this.connectionStatusService = connectionStatusService;
        this.creationService = creationService;
    }

    @GetMapping(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
    public ResponseEntity<ConnectionStatusMessage> permissionStatus(@PathVariable String permissionId) throws PermissionNotFoundException {
        var statusMessage = connectionStatusService.findConnectionStatusMessageById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        return ResponseEntity.ok(statusMessage);
    }

    @PostMapping(value = PATH_PERMISSION_REQUEST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(
            @RequestBody @Valid PermissionRequestForCreation permissionRequestForCreation
    ) throws ValidationException {
        LOGGER.info("Creating new permission request");
        var createdRequest = creationService.createAndValidatePermissionRequest(permissionRequestForCreation);
        var location = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
                .expand(createdRequest.permissionId());

        return ResponseEntity.created(location).body(createdRequest);
    }
}