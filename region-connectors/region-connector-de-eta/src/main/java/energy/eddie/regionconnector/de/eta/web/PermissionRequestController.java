package energy.eddie.regionconnector.de.eta.web;

import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.de.eta.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.de.eta.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.de.eta.service.PermissionRequestCreationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.CONNECTION_STATUS_STREAM;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;

/**
 * REST controller for handling permission request creation for the German (DE) ETA Plus region connector.
 * This endpoint is called by the EDDIE Button when a user initiates a connection.
 */
@RestController
public class PermissionRequestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    
    private final PermissionRequestCreationService permissionRequestCreationService;

    public PermissionRequestController(PermissionRequestCreationService permissionRequestCreationService) {
        this.permissionRequestCreationService = permissionRequestCreationService;
    }

    @PostMapping(
            value = PATH_PERMISSION_REQUEST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(
            @Valid @RequestBody PermissionRequestForCreation permissionRequestForCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        LOGGER.info("Received permission request creation for connection {}", 
                permissionRequestForCreation.connectionId());
        
        var permissionRequest = permissionRequestCreationService.createPermissionRequest(permissionRequestForCreation);
        
        return ResponseEntity.created(
                new UriTemplate(CONNECTION_STATUS_STREAM).expand(permissionRequest.permissionId())
        ).body(permissionRequest);
    }
}
