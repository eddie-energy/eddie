package energy.eddie.regionconnector.dk.energinet.web;

import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.dk.energinet.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.services.InvalidRefreshTokenException;
import energy.eddie.regionconnector.dk.energinet.services.PermissionCreationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.CONNECTION_STATUS_STREAM;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;


@RestController
public class PermissionRequestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final PermissionCreationService permissionCreationService;

    @Autowired
    public PermissionRequestController(PermissionCreationService permissionCreationService) {
        this.permissionCreationService = permissionCreationService;
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

    @PostMapping(value = PATH_PERMISSION_REQUEST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatedPermissionRequest> permissionRequest(@Valid @RequestBody PermissionRequestForCreation requestForCreation)
            throws DataNeedNotFoundException, UnsupportedDataNeedException, InvalidRefreshTokenException {
        var permissionId = permissionCreationService.createPermissionRequest(requestForCreation).permissionId();
        LOGGER.info("New Permission Request with PermissionId: {}", permissionId);

        var location = new UriTemplate(CONNECTION_STATUS_STREAM)
                .expand(permissionId);

        return ResponseEntity.created(location).body(new CreatedPermissionRequest(permissionId));
    }
}
