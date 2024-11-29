package energy.eddie.regionconnector.be.fluvius.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.be.fluvius.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.be.fluvius.service.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;

@RestController
public class PermissionRequestController {
    private static final String STATUS = "status";
    private final PermissionRequestService permissionRequestService;

    public PermissionRequestController(PermissionRequestService permissionRequestService) {
        this.permissionRequestService = permissionRequestService;
    }

    @PostMapping(
            value = PATH_PERMISSION_REQUEST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(@Valid @RequestBody PermissionRequestForCreation permissionRequestForCreation) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionRequest = permissionRequestService.createPermissionRequest(permissionRequestForCreation);
        return ResponseEntity.created(new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionRequest.permissionId()))
                             .body(permissionRequest);
    }

    @GetMapping(
            value = PATH_PERMISSION_STATUS_WITH_PATH_PARAM,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ConnectionStatusMessage> permissionStatus(@PathVariable String permissionId) throws PermissionNotFoundException {
        var statusMessage = permissionRequestService.findConnectionStatusMessageById(permissionId)
                                                    .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        return ResponseEntity.ok(statusMessage);
    }

    @GetMapping(
            value = "/callback/success",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String callbackSuccess(
            Model model
    ) {
        model.addAttribute(STATUS, "OK");
        return "authorization-callback";
    }
}
