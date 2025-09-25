package energy.eddie.regionconnector.si.moj.elektro.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.si.moj.elektro.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.si.moj.elektro.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.si.moj.elektro.service.PermissionRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;

@Controller
public class PermissionRequestController {

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
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(@Valid @RequestBody PermissionRequestForCreation permissionRequestForCreation)
            throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionRequest = permissionRequestService.createPermissionRequest(permissionRequestForCreation);
        return ResponseEntity.created(
            new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(permissionRequest.permissionId())
        ).body(permissionRequest);
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
}
