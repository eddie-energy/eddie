package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.PatchPermissionDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.permission.DetailFetchingFailedException;
import energy.eddie.aiida.errors.permission.PermissionAlreadyExistsException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionUnfulfillableException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.services.PermissionService;
import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.aiida.QrCodeDto;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/permissions")
@Tag(name = "Permission Controller")
public class PermissionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionController.class);
    private static final String REVOKE_PERMISSION_EXAMPLE_RETURN_JSON = "{\"permissionId\":\"a4dc1bad-b9fe-47ae-9336-690cfb4aada9\",\"status\":\"REVOKED\",\"serviceName\":\"My Energy Visualization Service\",\"dataNeedId\":\"DATA_NEED_ID\",\"startTime\":\"2023-10-01T08:00:00Z\",\"expirationTime\":\"2023-10-31T20:00:00Z\",\"grantTime\":\"2023-10-01T08:00:00Z\",\"revokeTime\":\"2023-10-20T08:00:00Z\",\"connectionId\":\"SomeRandomString\",\"requestedCodes\":\"[\\\"1-0:1.8.0\\\",\\\"1-0:1.7.0\\\"]\"}}";
    private final PermissionService permissionService;

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "Get all permissions sorted by grant time desc", description = "Get all permissions sorted by their grant time descending.", operationId = "getPermissionsSorted", tags = {"permission"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "successful operation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Permission.class))))})
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Permission>> getAllPermissions() throws InvalidUserException {
        return ResponseEntity.ok(permissionService.getAllPermissionsSortedByGrantTime());
    }

    @Operation(summary = "Set up new permission", description = "Set up a new permission with data from e.g. a QR code.", operationId = "setupNewPermission", tags = {"permission"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Permission.class))}),
            @ApiResponse(responseCode = "400", description = "Request body cannot be read or is missing fields.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "409", description = "Permission cannot be fulfilled, e.g. because the requested data is not available.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class)))})
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Permission> setupNewPermission(@Valid @RequestBody QrCodeDto qrCodeDto) throws PermissionAlreadyExistsException, PermissionUnfulfillableException, DetailFetchingFailedException, InvalidUserException {
        LOGGER.debug("Got new permission request {}", qrCodeDto);

        var permission = permissionService.setupNewPermission(qrCodeDto);

        var location = new UriTemplate("/permissions/{permissionId}").expand(permission.id());

        return ResponseEntity.created(location).body(permission);
    }

    @Operation(summary = "Update a permission", description = "Accept, reject or revoke a permission.", operationId = "updatePermission", tags = {"permission"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Permission.class), examples = @ExampleObject(value = REVOKE_PERMISSION_EXAMPLE_RETURN_JSON))}),
            @ApiResponse(responseCode = "400", description = "Invalid operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "404", description = "Permission not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "409", description = "Permission cannot be updated as it's not in an eligible state.", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))})})
    @PatchMapping(value = "/{permissionId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Permission> updatePermission(
            @Valid @RequestBody PatchPermissionDto patchDto,
            @Parameter(name = "permissionId", description = "Unique ID of the permission", example = "f38a1953-ae7a-480c-814f-1cca3989981e") @PathVariable UUID permissionId
    ) throws PermissionStateTransitionException, PermissionNotFoundException, DetailFetchingFailedException, UnauthorizedException, InvalidUserException {
        LOGGER.atInfo()
              // Validate that it's a real permission ID and not some malicious string
              .addArgument(() -> permissionId)
              .addArgument(patchDto::operation)
              .log("Got request to update permission '{}' with operation {}");

        var permission = switch (patchDto.operation()) {
            case ACCEPT -> permissionService.acceptPermission(permissionId, patchDto.dataSourceId());
            case REJECT -> permissionService.rejectPermission(permissionId);
            case REVOKE -> permissionService.revokePermission(permissionId);
        };
        return ResponseEntity.ok(permission);
    }
}
