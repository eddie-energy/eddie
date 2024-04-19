package energy.eddie.regionconnector.aiida.web;

import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.services.PermissionCreationValidationSendingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;

@RestController
public class PermissionRequestController {
    private final PermissionCreationValidationSendingService service;

    @Autowired
    public PermissionRequestController(PermissionCreationValidationSendingService service) {
        this.service = service;
    }

    @PostMapping(value = PATH_PERMISSION_REQUEST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PermissionDto> createPermissionRequest(
            @Valid @RequestBody PermissionRequestForCreation permissionRequestForCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionDto = service.createValidateAndSendPermissionRequest(permissionRequestForCreation);
        var permissionId = permissionDto.permissionId();

        var location = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
                .expand(permissionId);

        return ResponseEntity.created(location).body(permissionDto);
    }
}
