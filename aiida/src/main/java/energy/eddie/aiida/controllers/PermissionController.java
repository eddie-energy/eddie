package energy.eddie.aiida.controllers;

import energy.eddie.aiida.dtos.ErrorResponse;
import energy.eddie.aiida.dtos.PatchOperation;
import energy.eddie.aiida.dtos.PatchPermissionDto;
import energy.eddie.aiida.dtos.PermissionDto;
import energy.eddie.aiida.errors.InvalidPatchOperationException;
import energy.eddie.aiida.errors.PermissionAlreadyExistsException;
import energy.eddie.aiida.errors.PermissionStartFailedException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.services.PermissionService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/permissions")
@OpenAPIDefinition(info = @Info(title = "Permissions API", version = "1.0", description = "Manage permissions"))
public class PermissionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionController.class);
    private final PermissionService permissionService;
    private static final String REVOKE_PERMISSION_EXAMPLE_RETURN_JSON = "{\"permissionId\":\"a4dc1bad-b9fe-47ae-9336-690cfb4aada9\",\"status\":\"REVOKED\",\"serviceName\":\"My Energy Visualization Service\",\"dataNeedId\":\"DATA_NEED_ID\",\"startTime\":\"2023-10-01T08:00:00Z\",\"expirationTime\":\"2023-10-31T20:00:00Z\",\"grantTime\":\"2023-10-01T08:00:00Z\",\"revokeTime\":\"2023-10-20T08:00:00Z\",\"connectionId\":\"SomeRandomString\",\"requestedCodes\":\"[\\\"1-0:1.8.0\\\",\\\"1-0:1.7.0\\\"]\",\"kafkaStreamingConfig\":{\"bootstrapServers\":\"localhost:9092\",\"dataTopic\":\"SomeDataTopic\",\"statusTopic\":\"SomeStatusTopic\",\"subscribeTopic\":\"SomeSubscribeTopic\"}}";

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "Get all permissions sorted by grant time desc", description = "Get all permissions sorted by their grant time descending.",
            operationId = "getPermissionsSorted", tags = {"permission"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "successful operation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Permission.class))))})
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissionsSortedByGrantTime());
    }

    @Operation(summary = "Set up new permission", description = "Set up a new permission with data from e.g. a QR code.",
            operationId = "setupNewPermission", tags = {"permission"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Permission.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid body supplied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Failed to start permission", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"errors\":[\"Failed to start permission, please try again later.\"]}")))})
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Permission> setupNewPermission(@Valid @RequestBody PermissionDto newPermission)
            throws PermissionStartFailedException, PermissionAlreadyExistsException {
        LOGGER.debug("Got new permission request {}", newPermission);

        var permission = permissionService.setupNewPermission(newPermission);

        var location = new UriTemplate("/permissions/{permissionId}")
                .expand(permission.permissionId());

        return ResponseEntity.created(location).body(permission);
    }

    @Operation(summary = "Revoke a permission", description = "Revoke a permission and stop streaming data.",
            operationId = "revokePermission", tags = {"permission"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Permission.class), examples = @ExampleObject(value = REVOKE_PERMISSION_EXAMPLE_RETURN_JSON))}),
            @ApiResponse(responseCode = "400", description = "Invalid operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "404", description = "Permission not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))}),
            @ApiResponse(responseCode = "405", description = "Permission not eligible for revocation, e.g. it is already expired or terminated.", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))})
    })
    @PatchMapping(value = "/{permissionId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Permission> revokePermission(@Valid @RequestBody PatchPermissionDto patchPermissionDto,
                                                       @Parameter(name = "permissionId", description = "Unique ID of the permission", example = "f38a1953-ae7a-480c-814f-1cca3989981e")
                                                       @PathVariable String permissionId) {
        if (patchPermissionDto.operation() != PatchOperation.REVOKE_PERMISSION) {
            throw new InvalidPatchOperationException();
        }

        return ResponseEntity.ok(permissionService.revokePermission(permissionId));
    }
}
