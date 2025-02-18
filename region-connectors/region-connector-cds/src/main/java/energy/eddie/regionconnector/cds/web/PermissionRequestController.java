package energy.eddie.regionconnector.cds.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.cds.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.cds.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.cds.exceptions.UnknownPermissionAdministratorException;
import energy.eddie.regionconnector.cds.services.PermissionRequestCreationService;
import energy.eddie.regionconnector.cds.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;

@Controller
public class PermissionRequestController {
    private final PermissionRequestCreationService creationService;
    private final PermissionRequestService permissionRequestService;

    public PermissionRequestController(
            PermissionRequestCreationService creationService,
            PermissionRequestService permissionRequestService
    ) {
        this.creationService = creationService;
        this.permissionRequestService = permissionRequestService;
    }

    @PostMapping(PATH_PERMISSION_REQUEST)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(@RequestBody PermissionRequestForCreation creationDto) throws DataNeedNotFoundException, UnknownPermissionAdministratorException, UnsupportedDataNeedException {
        var pr = creationService.createPermissionRequest(creationDto);
        var location = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM).expand(pr.permissionId());
        return ResponseEntity.created(location).body(pr);
    }

    @GetMapping(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
    public ResponseEntity<ConnectionStatusMessage> permissionStatus(@PathVariable String permissionId) throws PermissionNotFoundException {
        return ResponseEntity.ok(permissionRequestService.getConnectionStatusMessage(permissionId));
    }
}
