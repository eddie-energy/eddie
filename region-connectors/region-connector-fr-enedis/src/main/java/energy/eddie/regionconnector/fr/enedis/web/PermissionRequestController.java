package energy.eddie.regionconnector.fr.enedis.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.web.CustomElementPath;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriTemplate;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.BASE_PATH;

@RestController
@RequestMapping(BASE_PATH)
public class PermissionRequestController {
    private static final String CE_JS = "/ce.js";
    private static final String CE_PRODUCTION_PATH = "/public" + BASE_PATH + CE_JS;
    private static final String[] CE_DEV_PATHS = new String[]{
            "./region-connectors/region-connector-fr-enedis/src/main/resources" + CE_PRODUCTION_PATH,
            "./src/main/resources" + CE_PRODUCTION_PATH
    };

    @SuppressWarnings("java:S1075") // Is used to build the location header
    private static final String PERMISSION_STATUS_PATH = "/permission-status";
    private final PermissionRequestService permissionRequestService;

    public PermissionRequestController(PermissionRequestService permissionRequestService) {
        this.permissionRequestService = permissionRequestService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(ZonedDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                ZonedDateTime zonedDateTime = LocalDate.parse(text, DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneOffset.UTC);
                setValue(zonedDateTime);
            }
        });
    }

    @GetMapping(value = "/ce.js", produces = "text/javascript")
    public String javascriptConnectorElement() {
        var cePath = new CustomElementPath(getClass(), CE_DEV_PATHS, CE_PRODUCTION_PATH);
        try (InputStream in = cePath.getCEInputStream(false)) { // Always use production paths
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = PERMISSION_STATUS_PATH + "/{permissionId}")
    public ConnectionStatusMessage permissionStatus(@PathVariable String permissionId) {
        Optional<ConnectionStatusMessage> connectionStatusMessage =
                permissionRequestService.findConnectionStatusMessageById(permissionId);
        if (connectionStatusMessage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find permission request");
        }
        return connectionStatusMessage.get();
    }

    @PostMapping(
            value = "/permission-request",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequestJsonBody(
            @RequestBody
            @Valid
            PermissionRequestForCreation permissionRequest
    ) throws StateTransitionException {
        return createPermissionRequest(permissionRequest);
    }

    @PostMapping(
            value = "/permission-request",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequestUrlEncoded(
            @ModelAttribute
            @Valid
            PermissionRequestForCreation permissionRequest
    ) throws StateTransitionException {
        return createPermissionRequest(permissionRequest);
    }

    private ResponseEntity<CreatedPermissionRequest> createPermissionRequest(PermissionRequestForCreation permissionRequest) throws StateTransitionException {
        CreatedPermissionRequest createdPermissionRequest = permissionRequestService.createPermissionRequest(permissionRequest);
        URI location = new UriTemplate("{statusPath}/{permissionId}")
                .expand(PERMISSION_STATUS_PATH, createdPermissionRequest.permissionId());
        return ResponseEntity
                .created(location)
                .body(createdPermissionRequest);
    }

    @GetMapping(value = "/authorization-callback")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> callback(@RequestParam("state") String stateString, @RequestParam("usage_point_id") String usagePointId)
            throws StateTransitionException, PermissionNotFoundException {
        permissionRequestService.authorizePermissionRequest(stateString, usagePointId);
        return ResponseEntity.ok("Access Granted. You can close this tab now.");
    }
}
