package energy.eddie.regionconnector.es.datadis.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.BASE_PATH;

@RestController
@RequestMapping(BASE_PATH)
public class PermissionController {
    private static final String CE_JS = "ce.js";
    private static final String[] CE_DEV_PATHS = new String[]{
            "./region-connectors/region-connector-es-datadis/src/main/resources/public" + BASE_PATH + CE_JS,
            "./src/main/resources/public" + BASE_PATH + CE_JS
    };
    private static final String CE_PRODUCTION_PATH = "/public" + BASE_PATH + CE_JS;
    // this path will stay hard-coded
    @SuppressWarnings("java:S1075")
    private static final String PERMISSION_STATUS_PATH = "/permission-status";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionController.class);
    private final Environment environment;
    private final PermissionRequestService service;

    @Autowired
    public PermissionController(Environment environment, PermissionRequestService service) {
        this.environment = environment;
        this.service = service;
    }

    private static String findCEDevPath() throws FileNotFoundException {
        for (String ceDevPath : CE_DEV_PATHS) {
            if (new File(ceDevPath).exists()) {
                return ceDevPath;
            }
        }
        throw new FileNotFoundException();
    }

    private InputStream getCEInputStream() throws FileNotFoundException {
        return !environment.matchesProfiles("dev")
                ? new FileInputStream(findCEDevPath())
                : Objects.requireNonNull(getClass().getResourceAsStream(CE_PRODUCTION_PATH));
    }

    @GetMapping(value = "/ce.js", produces = "text/javascript")
    public String javascriptConnectorElement() {
        try (InputStream in = getCEInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/permission-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectionStatusMessage> permissionStatus(@RequestParam String permissionId) {
        var statusMessage = service.findConnectionStatusMessageById(permissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find permission request"));

        return ResponseEntity.ok(statusMessage);
    }

    @PostMapping(value = "/permission-request", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> requestPermission(@Valid @ModelAttribute PermissionRequestForCreation requestForCreation) {
        LOGGER.info("request was: {}", requestForCreation);
        var permissionId = "TODO";

        var location = new UriTemplate("{statusPath}/{permissionId}")
                .expand(PERMISSION_STATUS_PATH, permissionId);
        return ResponseEntity.created(location).body(permissionId);
    }

    @PostMapping(value = "/permission-request/accepted")
    public ResponseEntity<String> acceptPermission(@RequestParam String permissionId) throws PermissionNotFoundException, StateTransitionException {
        service.acceptPermission(permissionId);
        return ResponseEntity.ok(permissionId);
    }

    @PostMapping(value = "/permission-request/rejected")
    public ResponseEntity<String> rejectPermission(@RequestParam String permissionId) throws PermissionNotFoundException, StateTransitionException {
        service.rejectPermission(permissionId);
        return ResponseEntity.ok(permissionId);
    }
}
