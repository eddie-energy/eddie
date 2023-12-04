package energy.eddie.regionconnector.aiida.web;

import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static energy.eddie.regionconnector.aiida.AiidaRegionConnector.BASE_PATH;

@RestController
@RequestMapping(BASE_PATH)
public class PermissionRequestController {
    private static final String CE_JS = "ce.js";
    private static final String CE_PRODUCTION_PATH = "/public" + BASE_PATH + CE_JS;
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final AiidaRegionConnectorService aiidaService;

    @Autowired
    public PermissionRequestController(AiidaRegionConnectorService aiidaService) {
        this.aiidaService = aiidaService;
    }

    @GetMapping(value = "/" + CE_JS, produces = "text/javascript")
    public String javascriptConnectorElement() throws IOException {
        try (InputStream in = getCEInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private InputStream getCEInputStream() {
        return getClass().getResourceAsStream(CE_PRODUCTION_PATH);
    }

    @CrossOrigin
    @PostMapping(value = "/permission-request", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PermissionDto> createPermissionRequest(
            @Valid @RequestBody PermissionRequestForCreation permissionRequestForCreation)
            throws StateTransitionException {
        LOGGER.info("Got new request for connectionId {}", permissionRequestForCreation.connectionId());
        return ResponseEntity.ok(aiidaService.createNewPermission(permissionRequestForCreation));
    }
}
