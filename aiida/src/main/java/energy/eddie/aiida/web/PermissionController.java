// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.connectionstatus.ConnectionStatusEventDto;
import energy.eddie.aiida.dtos.permission.*;
import energy.eddie.aiida.errors.SecretStoringException;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.datasource.IncompatibleDataSourceException;
import energy.eddie.aiida.errors.permission.*;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.services.AuthService;
import energy.eddie.aiida.services.ConnectionStatusService;
import energy.eddie.aiida.services.PermissionService;
import energy.eddie.aiida.streamers.ConnectionStatusEventStream;
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
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/permissions")
@Tag(name = "Permission Controller")
public class PermissionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionController.class);
    private final PermissionService permissionService;
    private final ConnectionStatusService connectionStatusService;
    private final ConnectionStatusEventStream connectionStatusEventStream;
    private final EventStream eventStream;
    private final AuthService authService;

    @Autowired
    public PermissionController(
            PermissionService permissionService,
            ConnectionStatusService connectionStatusService,
            ConnectionStatusEventStream connectionStatusEventStream,
            EventStream eventStream,
            AuthService authService
    ) {
        this.permissionService = permissionService;
        this.connectionStatusService = connectionStatusService;
        this.connectionStatusEventStream = connectionStatusEventStream;
        this.eventStream = eventStream;
        this.authService = authService;
    }

    @Operation(summary = "Get all permissions sorted by grant time desc", description = "Get all permissions sorted by their grant time descending.", operationId = "getPermissionsSorted", tags = {"permission"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "successful operation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PermissionDto.class))))})
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PermissionDto>> getAllPermissions() throws InvalidUserException {
        return ResponseEntity.ok(toDtos(permissionService.getAllPermissionsSortedByGrantTime()));
    }

    @Operation(summary = "Stream connection status changes", description = "Server-sent events whenever the connection status of a permission of the current user changes, e.g. once the connection to the EDDIE framework is established after accepting a permission.", operationId = "connectionStatusStream", tags = {"permission"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "successful operation", content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(implementation = ConnectionStatusEventDto.class)))})
    @GetMapping(path = "/connection-status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ConnectionStatusEventDto>> connectionStatusStream() throws InvalidUserException {
        var userId = authService.getCurrentUserId();
        return eventStream.toJson(connectionStatusEventStream.events(userId)
                                                             .map(event -> new ConnectionStatusEventDto(
                                                                     event.permissionId(),
                                                                     event.connectionStatus())));
    }

    @Operation(summary = "Get accepted inbound permissions", description = "Get accepted inbound permissions that can be used as a data source selection.", operationId = "getAcceptedInboundPermissions", tags = {"permission"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "successful operation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PermissionDto.class))))})
    @GetMapping(path = "/inbound/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PermissionDto>> getAcceptedInboundPermissions() throws InvalidUserException {
        return ResponseEntity.ok(toDtos(permissionService.getAcceptedInboundPermissions()));
    }

    @Operation(summary = "Fetch details of permission requests", description = "Fetches and validates the details of the permission requests contained in an AIIDA code. Nothing is persisted, so the AIIDA code can be used again until the permission requests are accepted or rejected.", operationId = "fetchPermissionDetails", tags = {"permission"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PendingPermissionDto.class)))}),
            @ApiResponse(responseCode = "400", description = "Request body cannot be read or is missing fields, or a permission was already accepted.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "409", description = "Permission(s) cannot be fulfilled, e.g. because the requested data is not available.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "503", description = "The details could not be fetched from the EDDIE framework.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class)))})
    @PostMapping(value = "/details", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PendingPermissionDto>> fetchPermissionDetails(@Valid @RequestBody AiidaPermissionRequestsDto permissionRequests) throws PermissionAlreadyExistsException, DetailFetchingFailedException, InvalidUserException, ActiveFcaPermissionAlreadyExistsException, PermissionStartInThePastException, PermissionDataNeedTypeNotSupportedException, LimitDefaultsNotAllowedException {
        LOGGER.debug("Got request to fetch details for {}", permissionRequests.permissionIds());
        return ResponseEntity.ok(permissionService.fetchPermissionDetails(permissionRequests));
    }

    @Operation(summary = "Accept an outbound permission request", description = "Accepts an outbound permission request from an AIIDA code and persists the permission together with the selected data source.", operationId = "acceptOutboundPermission", tags = {"permission"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Permission accepted", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = PermissionDto.class))}),
            @ApiResponse(responseCode = "400", description = "Not an outbound permission request, incompatible data source, invalid request body, or the permission was already accepted.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "404", description = "Data source not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "409", description = "Permission cannot be fulfilled.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "503", description = "The EDDIE framework could not be reached.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class)))})
    @PostMapping(value = "/{permissionId}/accept/outbound", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PermissionDto> acceptOutboundPermission(
            @Parameter(name = "permissionId", description = "Unique ID of the permission request", example = "f38a1953-ae7a-480c-814f-1cca3989981e") @PathVariable UUID permissionId,
            @Valid @RequestBody AcceptOutboundPermissionDto acceptDto
    ) throws PermissionAlreadyExistsException, DetailFetchingFailedException, InvalidUserException, ActiveFcaPermissionAlreadyExistsException, PermissionStartInThePastException, PermissionDataNeedTypeNotSupportedException, LimitDefaultsNotAllowedException, InvalidOutboundPermissionException, DataSourceNotFoundException, IncompatibleDataSourceException, SecretStoringException {
        LOGGER.info("Got request to accept outbound permission '{}'", permissionId);
        var permission = permissionService.acceptOutboundPermission(permissionId, acceptDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(connectionStatusService.toDto(permission));
    }

    @Operation(summary = "Accept an inbound permission request", description = "Accepts an inbound permission request from an AIIDA code and persists the permission. Its data source is created automatically.", operationId = "acceptInboundPermission", tags = {"permission"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Permission accepted", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = PermissionDto.class))}),
            @ApiResponse(responseCode = "400", description = "Not an inbound permission request, invalid request body, or the permission was already accepted.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "409", description = "Permission cannot be fulfilled.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "503", description = "The EDDIE framework could not be reached.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class)))})
    @PostMapping(value = "/{permissionId}/accept/inbound", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PermissionDto> acceptInboundPermission(
            @Parameter(name = "permissionId", description = "Unique ID of the permission request", example = "f38a1953-ae7a-480c-814f-1cca3989981e") @PathVariable UUID permissionId,
            @Valid @RequestBody AcceptInboundPermissionDto acceptDto
    ) throws PermissionAlreadyExistsException, DetailFetchingFailedException, InvalidUserException, ActiveFcaPermissionAlreadyExistsException, PermissionStartInThePastException, PermissionDataNeedTypeNotSupportedException, LimitDefaultsNotAllowedException, InvalidInboundPermissionException, AssetNotAllowedException, SecretStoringException {
        LOGGER.info("Got request to accept inbound permission '{}'", permissionId);
        var permission = permissionService.acceptInboundPermission(permissionId, acceptDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(connectionStatusService.toDto(permission));
    }

    @Operation(summary = "Reject a permission request", description = "Rejects a permission request from an AIIDA code at the EDDIE framework. Nothing is persisted.", operationId = "rejectPermission", tags = {"permission"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Permission request rejected"),
            @ApiResponse(responseCode = "400", description = "Invalid request body, or the permission was already accepted.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class)))})
    @PostMapping(value = "/{permissionId}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> rejectPermission(
            @Parameter(name = "permissionId", description = "Unique ID of the permission request", example = "f38a1953-ae7a-480c-814f-1cca3989981e") @PathVariable UUID permissionId,
            @Valid @RequestBody RejectPermissionDto rejectDto
    ) throws PermissionAlreadyExistsException {
        LOGGER.info("Got request to reject permission '{}'", permissionId);
        permissionService.rejectPermission(permissionId, rejectDto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a permission", description = "Revoke, or update the inbound message format or display name of an accepted permission.", operationId = "updatePermission", tags = {"permission"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = PermissionDto.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "404", description = "Permission not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))}),
            @ApiResponse(responseCode = "409", description = "Permission cannot be updated as it's not in an eligible state.", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EddieApiError.class))})})
    @PatchMapping(value = "/{permissionId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PermissionDto> updatePermission(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Permission patch request. Fields required depend on the selected operation.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PatchPermissionDto.class),
                            examples = {
                                    @ExampleObject(name = "Revoke permission",
                                            value = "{\"operation\":\"REVOKE\"}"),
                                    @ExampleObject(name = "Update inbound message format",
                                            value = "{\"operation\":\"UPDATE_INBOUND_MESSAGE_FORMAT\",\"inboundMessageFormat\":\"OPENADR_3_1\"}"),
                                    @ExampleObject(name = "Update display name",
                                            value = "{\"operation\":\"UPDATE_DISPLAY_NAME\",\"displayName\":\"My smart meter\"}")
                            })
            )
            @Valid @RequestBody PatchPermissionDto patchDto,
            @Parameter(name = "permissionId", description = "Unique ID of the permission", example = "f38a1953-ae7a-480c-814f-1cca3989981e") @PathVariable UUID permissionId
    ) throws PermissionStateTransitionException, PermissionNotFoundException, UnauthorizedException,
             InvalidUserException, MissingInboundMessageFormatException, MissingDisplayNameException,
             InvalidInboundPermissionException, InboundDataSourceInUseException {
        LOGGER.atInfo()
              // Validate that it's a real permission ID and not some malicious string
              .addArgument(() -> permissionId)
              .addArgument(patchDto::operation)
              .log("Got request to update permission '{}' with operation {}");

        var permission = switch (patchDto.operation()) {
            case REVOKE -> permissionService.revokePermission(permissionId);
            case UPDATE_INBOUND_MESSAGE_FORMAT -> permissionService.updateInboundMessageFormat(
                    permissionId,
                    patchDto.inboundMessageFormat()
            );
            case UPDATE_DISPLAY_NAME -> permissionService.updateDisplayName(
                    permissionId,
                    patchDto.displayName()
            );
        };
        return ResponseEntity.ok(connectionStatusService.toDto(permission));
    }

    private List<PermissionDto> toDtos(List<Permission> permissions) {
        return permissions.stream().map(connectionStatusService::toDto).toList();
    }
}
