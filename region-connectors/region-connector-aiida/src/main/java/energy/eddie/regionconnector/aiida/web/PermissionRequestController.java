package energy.eddie.regionconnector.aiida.web;

import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.aiida.dtos.PermissionDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static energy.eddie.regionconnector.aiida.AiidaRegionConnector.BASE_PATH;

@RestController
@RequestMapping(BASE_PATH)
public class PermissionRequestController {
    private static final String CE_JS = "ce.js";
    /**
     * We have to check two different paths depending if the Region-Connector is run by the core or in standalone.
     */
    private static final String[] CE_DEV_PATHS = new String[]{
            "./region-connectors/region-connector-aiida/src/main/resources/public" + BASE_PATH + CE_JS,
            "./src/main/resources/public" + BASE_PATH + CE_JS
    };
    private static final String CE_PRODUCTION_PATH = "/public" + BASE_PATH + CE_JS;
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final AiidaRegionConnectorService aiidaService;
    private final Environment environment;

    @Autowired
    public PermissionRequestController(Environment environment, AiidaRegionConnectorService aiidaService) {
        this.environment = environment;
        this.aiidaService = aiidaService;
    }

    private static String findCEDevPath() throws FileNotFoundException {
        for (String ceDevPath : CE_DEV_PATHS) {
            if (new File(ceDevPath).exists()) {
                return ceDevPath;
            }
        }
        throw new FileNotFoundException();
    }

    @GetMapping(value = "/ce.js", produces = "text/javascript")
    public String javascriptConnectorElement() throws IOException {
        try (InputStream in = getCEInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private InputStream getCEInputStream() throws FileNotFoundException {
        return !environment.matchesProfiles("dev")
                ? new FileInputStream(findCEDevPath())
                : Objects.requireNonNull(getClass().getResourceAsStream(CE_PRODUCTION_PATH));
    }

    @PostMapping(value = "/permission-request", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PermissionDto> createPermissionRequest(
            @Valid @RequestBody PermissionRequestForCreation permissionRequestForCreation)
            throws StateTransitionException {
        LOGGER.info("Got new request for connectionId {}", permissionRequestForCreation.connectionId());
        return ResponseEntity.ok(aiidaService.createNewPermission(permissionRequestForCreation));
    }
}
