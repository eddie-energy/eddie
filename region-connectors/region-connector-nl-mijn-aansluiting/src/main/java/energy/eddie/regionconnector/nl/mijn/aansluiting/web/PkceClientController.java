package energy.eddie.regionconnector.nl.mijn.aansluiting.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.nl.mijn.aansluiting.exceptions.NlValidationException;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import java.net.URI;
import java.net.URISyntaxException;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;

@RestController
public class PkceClientController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PkceClientController.class);
    private static final String SESSION_ID = "EDDIE-SESSION-ID";
    private final PermissionRequestService service;

    public PkceClientController(PermissionRequestService service) {
        this.service = service;
    }

    @PostMapping(value = PATH_PERMISSION_REQUEST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatedPermissionRequest> permissionRequest(
            @RequestBody @Valid PermissionRequestForCreation permissionRequest,
            HttpServletResponse response
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, NlValidationException {
        var newPermission = service.createPermissionRequest(permissionRequest);
        URI location = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
                .expand(newPermission.permissionId());

        Cookie cookie = new Cookie(SESSION_ID, newPermission.permissionId());
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return ResponseEntity
                .created(location)
                .body(newPermission);
    }

    @GetMapping(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
    public ResponseEntity<ConnectionStatusMessage> permissionRequestStatus(@PathVariable String permissionId) throws PermissionNotFoundException {
        var csm = service.connectionStatusMessage(permissionId);
        return ResponseEntity.ok(csm);
    }

    @GetMapping("/oauth2/code/mijn-aansluiting")
    public ResponseEntity<String> callback(
            HttpServletRequest request,
            @CookieValue(value = SESSION_ID) String permissionId
    ) throws URISyntaxException, PermissionNotFoundException {
        var queryString = request.getQueryString();
        if (Strings.isBlank(queryString)) {
            LOGGER.warn("Answer did not provide any query parameters.");
            return ResponseEntity.badRequest().body("Invalid answer. Please contact the service provider.");
        }

        URI fullUri = new URI(request.getRequestURI() + "?" + queryString);
        LOGGER.info("Full callback URI {}", fullUri);

        var message = "";
        var error = request.getParameter("error");
        if (error != null) {
            message = "%nAnswer included an error.%n%s: %s"
                    .formatted(error, request.getParameter("error_description"));
        }

        var status = service.receiveResponse(fullUri, permissionId);
        return switch (status) {
            case PermissionProcessStatus.ACCEPTED -> ResponseEntity.ok(
                    "Access granted. You can close this tab now.");
            case PermissionProcessStatus.REJECTED -> ResponseEntity.ok(
                    "Access rejected. You can close this tab now.");
            case PermissionProcessStatus.INVALID -> ResponseEntity.ok(
                    "Invalid answer. Please contact the service provider."
                    + message);
            case PermissionProcessStatus.UNABLE_TO_SEND -> ResponseEntity.ok(
                    "Permission request could not be sent to permission administrator. Please contact the service provider."
                    + message);
            default -> ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                                     .body("Response lead to unexpected status: " + status);
        };
    }
}
