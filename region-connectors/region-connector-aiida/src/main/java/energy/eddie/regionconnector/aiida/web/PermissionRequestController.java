package energy.eddie.regionconnector.aiida.web;

import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PermissionRequestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final AiidaRegionConnectorService aiidaService;

    @Autowired
    public PermissionRequestController(AiidaRegionConnectorService aiidaService) {
        this.aiidaService = aiidaService;
    }

    @PostMapping(value = "/permission-request", produces = MediaType.APPLICATION_JSON_VALUE)
    // TODO: --> use correct responseEntity as well @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PermissionDto> createPermissionRequest(
            @Valid @RequestBody PermissionRequestForCreation permissionRequestForCreation)
            throws StateTransitionException {
        LOGGER.info("Got new request for connectionId {}", permissionRequestForCreation.connectionId());

        return ResponseEntity.ok(aiidaService.createNewPermission(permissionRequestForCreation));
    }
}
