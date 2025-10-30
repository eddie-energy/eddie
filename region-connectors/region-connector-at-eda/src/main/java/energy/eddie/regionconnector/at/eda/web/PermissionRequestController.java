package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestCreationAndValidationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.CONNECTION_STATUS_STREAM;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;

@RestController
public class PermissionRequestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final PermissionRequestCreationAndValidationService creationService;

    public PermissionRequestController(PermissionRequestCreationAndValidationService creationService) {
        this.creationService = creationService;
    }

    @PostMapping(value = PATH_PERMISSION_REQUEST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(
            @RequestBody @Valid PermissionRequestForCreation permissionRequestForCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        LOGGER.info("Creating new permission request");
        var createdRequest = creationService.createAndValidatePermissionRequest(permissionRequestForCreation);
        var location = new UriTemplate(CONNECTION_STATUS_STREAM)
                .expand(createdRequest.permissionId());

        return ResponseEntity.created(location).body(createdRequest);
    }
}
