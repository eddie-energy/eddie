package energy.eddie.regionconnector.es.datadis.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.BASE_PATH;

@RestController
@RequestMapping(BASE_PATH)
public class PermissionController {
    private static final String CE_JS = "/ce.js";
    private static final String CE_PRODUCTION_PATH = "/public" + BASE_PATH + CE_JS;
    // this path will stay hard-coded
    @SuppressWarnings("java:S1075")
    private static final String PERMISSION_STATUS_PATH = "/permission-status";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionController.class);
    private final PermissionRequestService service;

    @Autowired
    public PermissionController(PermissionRequestService service) {
        this.service = service;
    }

    /**
     * Registers custom deserializers for {@link PermissionRequestForCreation} fields.
     */
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

    private InputStream getCEInputStream() {
        return getClass().getResourceAsStream(CE_PRODUCTION_PATH);
    }

    @GetMapping(value = CE_JS, produces = "text/javascript")
    public String javascriptConnectorElement() {
        try (InputStream in = getCEInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = PERMISSION_STATUS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectionStatusMessage> permissionStatus(@RequestParam String permissionId) throws PermissionNotFoundException {
        var statusMessage = service.findConnectionStatusMessageById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        return ResponseEntity.ok(statusMessage);
    }

    @PostMapping(value = "/permission-request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> requestPermission(@Valid @ModelAttribute PermissionRequestForCreation requestForCreation) throws StateTransitionException {
        LOGGER.info("request was: {}", requestForCreation);

        var permissionRequest = service.createAndSendPermissionRequest(requestForCreation);

        String permissionId = permissionRequest.permissionId();
        var location = new UriTemplate("{statusPath}/{permissionId}")
                .expand(PERMISSION_STATUS_PATH, permissionId);

        return ResponseEntity.created(location).body(Map.of("permissionId", permissionId));
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
