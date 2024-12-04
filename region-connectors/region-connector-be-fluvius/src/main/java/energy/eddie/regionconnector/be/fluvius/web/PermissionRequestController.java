package energy.eddie.regionconnector.be.fluvius.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.*;

@Controller
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

    @GetMapping(PATH_PERMISSION_ACCEPTED)
    public String callbackSuccess(@PathVariable String permissionId, Model model) {
        return handlePermissionCallback(permissionId, model, PermissionProcessStatus.ACCEPTED);
    }

    @GetMapping(PATH_PERMISSION_REJECTED)
    public String callbackRejected(@PathVariable String permissionId, Model model) {
        return handlePermissionCallback(permissionId, model, PermissionProcessStatus.REJECTED);
    }

    private String handlePermissionCallback(String permissionId, Model model, PermissionProcessStatus status) {
        try {
            var wasAccepted = permissionRequestService.acceptOrRejectPermissionRequest(permissionId, status);
            if (wasAccepted) {
                model.addAttribute(STATUS, "OK");
            } else {
                model.addAttribute(STATUS, "DENIED");
            }
        } catch (PermissionNotFoundException e) {
            model.addAttribute(STATUS, "ERROR");
        }
        return "authorization-callback";
    }
}
