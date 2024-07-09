package energy.eddie.regionconnector.es.datadis.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata;
import energy.eddie.regionconnector.es.datadis.dtos.CreatedPermissionRequest;
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
import org.springframework.http.HttpHeaders;
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

import static energy.eddie.regionconnector.shared.web.RestApiPaths.*;

@RestController
public class PermissionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionController.class);
    private final PermissionRequestService service;
    private final JwtUtil jwtUtil;

    @Autowired
    public PermissionController(
            PermissionRequestService service,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") JwtUtil jwtUtil
    ) {
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
     * Creates a new permission request. The included JWT token can be passed in the Authorization header, so that the
     * {@link energy.eddie.regionconnector.shared.security.JwtAuthorizationManager} will allow further state changing
     * requests if they include the token.
     */
    @PostMapping(value = PATH_PERMISSION_REQUEST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatedPermissionRequest> requestPermission(
            @Valid @RequestBody PermissionRequestForCreation requestForCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, JwtCreationFailedException {
        var permissionRequest = service.createAndSendPermissionRequest(requestForCreation);

        var permissionId = permissionRequest.permissionId();
        LOGGER.info("New Permission Request created with PermissionId {}", permissionId);

        var jwt = jwtUtil.createJwt(DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID, permissionId);

        var location = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
                .expand(permissionId);
        return ResponseEntity.created(location)
                             .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                             .body(permissionRequest);
    }

    @PatchMapping(value = PATH_PERMISSION_ACCEPTED)
    public ResponseEntity<String> acceptPermission(@PathVariable String permissionId) throws PermissionNotFoundException {
        service.acceptPermission(permissionId);
        return ResponseEntity.ok(permissionId);
    }

    @PatchMapping(value = PATH_PERMISSION_REJECTED)
    public ResponseEntity<String> rejectPermission(@PathVariable String permissionId) throws PermissionNotFoundException {
        service.rejectPermission(permissionId);
        return ResponseEntity.ok(permissionId);
    }
}
