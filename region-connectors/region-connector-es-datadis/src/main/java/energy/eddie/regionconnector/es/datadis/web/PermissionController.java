package energy.eddie.regionconnector.es.datadis.web;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;

@RestController
public class PermissionController {
    public static final String PATH_PERMISSION_ACCEPTED = PATH_PERMISSION_REQUEST + "/{permissionId}/accepted";
    public static final String PATH_PERMISSION_REJECTED = PATH_PERMISSION_REQUEST + "/{permissionId}/rejected";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionController.class);
    private final PermissionRequestService service;
    private final JwtUtil jwtUtil;

    @Autowired
    public PermissionController(PermissionRequestService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registers custom deserializers for {@link PermissionRequestForCreation} fields.
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(ZonedDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                ZonedDateTime zonedDateTime = LocalDate.parse(text, DateTimeFormatter.ISO_DATE)
                                                       .atStartOfDay(ZoneOffset.UTC);
                setValue(zonedDateTime);
            }
        });
    }

    @GetMapping(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
    public ResponseEntity<ConnectionStatusMessage> permissionStatus(@PathVariable String permissionId) throws PermissionNotFoundException {
        var statusMessage = service.findConnectionStatusMessageById(permissionId)
                                   .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        return ResponseEntity.ok(statusMessage);
    }

    /**
     * Creates a new permission request and sets/updates the JWT token as cookie to include this new permissionId, so
     * that the {@link energy.eddie.regionconnector.shared.security.JwtAuthorizationManager} will grant allow further
     * state changing requests if they include the JWT cookie.
     */
    @PostMapping(value = PATH_PERMISSION_REQUEST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> requestPermission(
            @Valid @RequestBody PermissionRequestForCreation requestForCreation,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, StateTransitionException, JwtCreationFailedException {
        var permissionRequest = service.createAndSendPermissionRequest(requestForCreation);

        String permissionId = permissionRequest.permissionId();
        LOGGER.info("New Permission Request created with PermissionId {}", permissionId);

        jwtUtil.setJwtCookie(request, response, DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID, permissionId);

        var location = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
                .expand(permissionId);
        return ResponseEntity.created(location).body(Map.of("permissionId", permissionId));
    }

    @PatchMapping(value = PATH_PERMISSION_ACCEPTED)
    public ResponseEntity<String> acceptPermission(@PathVariable String permissionId) throws PermissionNotFoundException {
        service.acceptPermission(permissionId);
        return ResponseEntity.ok(permissionId);
    }

    @PatchMapping(value = PATH_PERMISSION_REJECTED)
    public ResponseEntity<String> rejectPermission(@PathVariable String permissionId) throws PermissionNotFoundException, StateTransitionException {
        service.rejectPermission(permissionId);
        return ResponseEntity.ok(permissionId);
    }
}
