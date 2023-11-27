package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestCreationService;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.BASE_PATH;

@RestController
@RequestMapping(EdaRegionConnectorMetadata.BASE_PATH)
public class PermissionRequestController {

    private static final String CE_JS = "/ce.js";
    private static final String CE_PRODUCTION_PATH = "/public" + BASE_PATH + CE_JS;
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final PermissionRequestService permissionRequestService;
    private final PermissionRequestCreationService creationService;

    public PermissionRequestController(PermissionRequestService permissionRequestService, PermissionRequestCreationService creationService) {
        this.permissionRequestService = permissionRequestService;
        this.creationService = creationService;
    }

    @GetMapping(value = "/permission-status")
    public ConnectionStatusMessage permissionStatus(@RequestParam String permissionId) {
        Optional<ConnectionStatusMessage> connectionStatusMessage = permissionRequestService.findConnectionStatusMessageById(permissionId);
        if (connectionStatusMessage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find permission request");
        }
        return connectionStatusMessage.get();
    }

    @GetMapping(value = CE_JS, produces = "text/javascript")
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
        LOGGER.info("Creating new permission request");
        return creationService.createAndSendPermissionRequest(permissionRequestForCreation);
    }

    private InputStream getCEInputStream() {
        return getClass().getResourceAsStream(CE_PRODUCTION_PATH);
    }
}