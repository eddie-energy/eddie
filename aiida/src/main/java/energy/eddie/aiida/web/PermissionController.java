// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

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
import energy.eddie.api.agnostic.aiida.AiidaPermissionRequestsDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static energy.eddie.aiida.dtos.PatchPermissionDto.*;

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

    @Operation(summary = "Set up new permissions", description = "Set up a new permissions with data from e.g. a QR code.", operationId = "setupNewPermission", tags = {"permission"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Permission.class))}),
            @ApiResponse(responseCode = "400", description = "Request body cannot be read or is missing fields.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "409", description = "Permission(s) cannot be fulfilled, e.g. because the requested data is not available.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class)))})
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Permission>> setupNewPermissions(@Valid @RequestBody AiidaPermissionRequestsDto permissionRequests) throws PermissionAlreadyExistsException, PermissionUnfulfillableException, DetailFetchingFailedException, InvalidUserException {
        LOGGER.debug("Got new permission request {}", permissionRequests);

        var permission = permissionService.setupNewPermissions(permissionRequests);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(permission);
    }

    @Operation(summary = "Update a permission", description = "Accept, reject, revoke, or update the inbound message format of a permission.", operationId = "updatePermission", tags = {"permission"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Permission.class), examples = @ExampleObject(value = REVOKE_PERMISSION_EXAMPLE_RETURN_JSON))}),
            @ApiResponse(responseCode = "400", description = "Invalid operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "404", description = "Permission not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "409", description = "Permission cannot be updated as it's not in an eligible state.", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))})})
    @PatchMapping(value = "/{permissionId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Permission> updatePermission(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Permission patch request. Fields required depend on the selected operation.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PatchPermissionDto.class),
                            examples = {
                                    @ExampleObject(name = "Accept permission",
                                            value = "{\"operation\":\"ACCEPT\",\"dataSourceId\":\"51d0a13e-688a-454d-acab-7a6b2951cde2\"}"),
                                    @ExampleObject(name = "Accept inbound permission with message format",
                                            value = "{\"operation\":\"ACCEPT\",\"dataSourceId\":\"51d0a13e-688a-454d-acab-7a6b2951cde2\",\"inboundMessageFormat\":\"OPENADR_3\"}"),
                                    @ExampleObject(name = "Update inbound message format",
                                            value = "{\"operation\":\"UPDATE_INBOUND_MESSAGE_FORMAT\",\"inboundMessageFormat\":\"OPENADR_3\"}")
                            })
            )
            @Valid @RequestBody PatchPermissionDto patchDto,
            @Parameter(name = "permissionId", description = "Unique ID of the permission", example = "f38a1953-ae7a-480c-814f-1cca3989981e") @PathVariable UUID permissionId
    ) throws PermissionStateTransitionException, PermissionNotFoundException, DetailFetchingFailedException, UnauthorizedException, InvalidUserException {
        var permission = switch (patchDto) {
            case Accept(var dataSourceId, var inboundMessageFormat) ->
                    permissionService.acceptPermission(permissionId, dataSourceId, inboundMessageFormat);
            case Reject() -> permissionService.rejectPermission(permissionId);
            case Revoke() -> permissionService.revokePermission(permissionId);
            case UpdateInboundMessageFormat(var inboundMessageFormat) ->
                    permissionService.updateInboundMessageFormat(permissionId, inboundMessageFormat);
            default -> throw new IllegalStateException("Unsupported PatchPermissionDto instance %s"
                                                               .formatted(patchDto.getClass().getName()));
        };
        return ResponseEntity.ok(permission);
    }
}
