package energy.eddie.regionconnector.dk.energinet.web;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.dk.energinet.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import energy.eddie.regionconnector.dk.energinet.services.PermissionRequestService;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


@RestController
public class PermissionRequestController {
    // this path will stay hard-coded
    @SuppressWarnings("java:S1075")
    private static final String PERMISSION_STATUS_PATH = "/permission-status";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final PermissionRequestService service;

    @Autowired
    public PermissionRequestController(PermissionRequestService service) {
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

        binder.registerCustomEditor(PeriodResolutionEnum.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                PeriodResolutionEnum period = PeriodResolutionEnum.fromString(text);
                setValue(period);
            }
        });
    }

    @GetMapping(PERMISSION_STATUS_PATH + "/{permissionId}")
    public ResponseEntity<ConnectionStatusMessage> permissionStatus(@PathVariable String permissionId) throws PermissionNotFoundException {
        var statusMessage = service.findConnectionStatusMessageById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        return ResponseEntity.ok(statusMessage);
    }

    @PostMapping(value = "/permission-request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatedPermissionRequest> permissionRequest(@Valid @ModelAttribute PermissionRequestForCreation requestForCreation)
            throws StateTransitionException {
        LOGGER.info("requestForCreation was: {}", requestForCreation);

        var permissionId = service.createAndSendPermissionRequest(requestForCreation).permissionId();

        var location = new UriTemplate("{statusPath}/{permissionId}")
                .expand(PERMISSION_STATUS_PATH, permissionId);

        return ResponseEntity.created(location).body(new CreatedPermissionRequest(permissionId));
    }
}
