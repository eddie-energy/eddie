package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestCreationService;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnector.BASE_PATH;

@RestController
@RequestMapping(BASE_PATH)
public class PermissionRequestController {

    private static final String CE_JS = "ce.js";
    /*
    We have to check two different paths depending on if the Region-Connector is run by the core or in standalone.
     */
    private static final String[] CE_DEV_PATHS = new String[]{
            "./region-connectors/region-connector-at-eda/src/main/resources/public" + BASE_PATH + CE_JS,
            "./src/main/resources/public" + BASE_PATH + CE_JS
    };
    private static final String CE_PRODUCTION_PATH = "/public" + BASE_PATH + CE_JS;
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final Environment environment;
    private final PermissionRequestService permissionRequestService;
    private final PermissionRequestCreationService creationService;

    public PermissionRequestController(Environment environment, PermissionRequestService permissionRequestService, PermissionRequestCreationService creationService) {
        this.environment = environment;
        this.permissionRequestService = permissionRequestService;
        this.creationService = creationService;
    }

    private static String findCEDevPath() throws FileNotFoundException {
        for (String ceDevPath : CE_DEV_PATHS) {
            if (new File(ceDevPath).exists()) {
                return ceDevPath;
            }
        }
        throw new FileNotFoundException();
    }

    @GetMapping(value = "/permission-status")
    public ConnectionStatusMessage permissionStatus(@RequestParam String permissionId) {
        Optional<ConnectionStatusMessage> connectionStatusMessage = permissionRequestService.findConnectionStatusMessageById(permissionId);
        if (connectionStatusMessage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find permission request");
        }
        return connectionStatusMessage.get();
    }

    @GetMapping(value = "/ce.js", produces = "text/javascript")
    public String javascriptConnectorElement() {
        try (InputStream in = getCEInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "/permission-request", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedPermissionRequest createPermissionRequest(
            @ModelAttribute @Valid PermissionRequestForCreation permissionRequestForCreation
    ) throws StateTransitionException {
        LOGGER.info("Creating new permission request {}", permissionRequestForCreation);
        return creationService.createAndSendPermissionRequest(permissionRequestForCreation);
    }

    private InputStream getCEInputStream() throws FileNotFoundException {
        return !environment.matchesProfiles("dev")
                ? new FileInputStream(findCEDevPath())
                : Objects.requireNonNull(getClass().getResourceAsStream(CE_PRODUCTION_PATH));
    }
}