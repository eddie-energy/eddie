package energy.eddie.regionconnector.fi.fingrid.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.fi.fingrid.config.FingridConfiguration;
import energy.eddie.regionconnector.fi.fingrid.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.dtos.OrganisationInformation;
import energy.eddie.regionconnector.fi.fingrid.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fi.fingrid.services.PermissionCreationService;
import energy.eddie.regionconnector.fi.fingrid.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.*;

@RestController
public class PermissionRequestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final PermissionCreationService creationService;
    private final FingridConfiguration fingridConfiguration;
    private final PermissionRequestService permissionRequestService;

    public PermissionRequestController(
            PermissionCreationService creationService,
            FingridConfiguration fingridConfiguration,
            PermissionRequestService permissionRequestService
    ) {
        this.creationService = creationService;
        this.fingridConfiguration = fingridConfiguration;
        this.permissionRequestService = permissionRequestService;
    }

    @GetMapping(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
    public ResponseEntity<ConnectionStatusMessage> permissionStatus(@PathVariable String permissionId) throws PermissionNotFoundException {
        return ResponseEntity.ok(permissionRequestService.connectionStatusMessage(permissionId));
    }

    @GetMapping(value = "/organisation-information")
    public ResponseEntity<OrganisationInformation> organisationInformation() {
        return ResponseEntity.ok(new OrganisationInformation(fingridConfiguration.organisationUser(),
                                                             fingridConfiguration.organisationName()));
    }

    @PostMapping(value = PATH_PERMISSION_REQUEST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(
            @RequestBody @Valid PermissionRequestForCreation permissionRequestForCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, JwtCreationFailedException {
        LOGGER.info("Creating new permission request");
        var createdRequest = creationService.createAndValidatePermissionRequest(permissionRequestForCreation);
        var permissionId = createdRequest.permissionId();
        var location = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
                .expand(permissionId);

        return ResponseEntity.created(location).body(createdRequest);
    }

    @PatchMapping(value = PATH_PERMISSION_ACCEPTED)
    public ResponseEntity<String> accepted(@PathVariable String permissionId) throws PermissionNotFoundException, PermissionStateTransitionException {
        creationService.acceptOrReject(permissionId, PermissionProcessStatus.ACCEPTED);
        return ResponseEntity.ok(permissionId);
    }

    @PatchMapping(value = PATH_PERMISSION_REJECTED)
    public ResponseEntity<String> rejected(@PathVariable String permissionId) throws PermissionNotFoundException, PermissionStateTransitionException {
        creationService.acceptOrReject(permissionId, PermissionProcessStatus.REJECTED);
        return ResponseEntity.ok(permissionId);
    }
}
