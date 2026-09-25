// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.connectionlimit.UpdateConnectionLimitMonitoringDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.connectionlimit.ConnectionLimitMonitoringNotAllowedException;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitMonitoring;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitMonitoringService;
import energy.eddie.api.agnostic.EddieApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/connection-limit-monitoring")
@Tag(name = "Connection Limit Monitoring Controller")
public class ConnectionLimitMonitoringController {
    private final ConnectionLimitMonitoringService connectionLimitMonitoringService;

    public ConnectionLimitMonitoringController(ConnectionLimitMonitoringService connectionLimitMonitoringService) {
        this.connectionLimitMonitoringService = connectionLimitMonitoringService;
    }

    @Operation(
            summary = "Get connection limit monitoring",
            description = """
                    Returns the data source that is monitored for connection limit violations of the permission.
                    Returns 204 if the permission supports monitoring but no data source is assigned.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Successful operation",
                         content = @Content(schema = @Schema(implementation = ConnectionLimitMonitoring.class))),
            @ApiResponse(responseCode = "204", description = "No data source assigned", content = @Content),
            @ApiResponse(responseCode = "400", description = "Permission does not support connection limit monitoring",
                         content = @Content(schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized User", content = @Content),
            @ApiResponse(responseCode = "404", description = "Permission not found",
                         content = @Content(schema = @Schema(implementation = EddieApiError.class)))
    })
    @GetMapping(path = "/{permissionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectionLimitMonitoring> getConnectionLimitMonitoring(
            @Parameter(description = "Permission ID to query the monitoring assignment for.",
                       example = "9921f327-f341-4bea-bf08-3cf2acc65bf3")
            @PathVariable UUID permissionId
    ) throws PermissionNotFoundException, InvalidUserException, ConnectionLimitMonitoringNotAllowedException {
        return connectionLimitMonitoringService.getConnectionLimitMonitoring(permissionId)
                                               .map(ResponseEntity::ok)
                                               .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(
            summary = "Assign connection limit monitoring",
            description = "Assigns an outbound data source to be monitored for connection limit violations of the permission."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Successful operation",
                         content = @Content(schema = @Schema(implementation = ConnectionLimitMonitoring.class))),
            @ApiResponse(responseCode = "400", description = "Permission or data source cannot be used for monitoring",
                         content = @Content(schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized User", content = @Content),
            @ApiResponse(responseCode = "404", description = "Permission or data source not found",
                         content = @Content(schema = @Schema(implementation = EddieApiError.class)))
    })
    @PutMapping(path = "/{permissionId}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectionLimitMonitoring> updateConnectionLimitMonitoring(
            @Parameter(description = "Permission ID to assign the monitoring data source for.",
                       example = "9921f327-f341-4bea-bf08-3cf2acc65bf3")
            @PathVariable UUID permissionId,
            @Valid @RequestBody UpdateConnectionLimitMonitoringDto updateDto
    ) throws PermissionNotFoundException, InvalidUserException, ConnectionLimitMonitoringNotAllowedException,
             DataSourceNotFoundException {
        return ResponseEntity.ok(connectionLimitMonitoringService.updateConnectionLimitMonitoring(permissionId,
                                                                                                  updateDto.dataSourceId()));
    }

    @Operation(
            summary = "Delete connection limit monitoring",
            description = "Clears the data source monitored for connection limit violations of the permission."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successful operation", content = @Content),
            @ApiResponse(responseCode = "400", description = "Permission does not support connection limit monitoring",
                         content = @Content(schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized User", content = @Content),
            @ApiResponse(responseCode = "404", description = "Permission not found",
                         content = @Content(schema = @Schema(implementation = EddieApiError.class)))
    })
    @DeleteMapping(path = "/{permissionId}")
    public ResponseEntity<Void> deleteConnectionLimitMonitoring(
            @Parameter(description = "Permission ID to clear the monitoring data source for.",
                       example = "9921f327-f341-4bea-bf08-3cf2acc65bf3")
            @PathVariable UUID permissionId
    ) throws PermissionNotFoundException, InvalidUserException, ConnectionLimitMonitoringNotAllowedException {
        connectionLimitMonitoringService.deleteConnectionLimitMonitoring(permissionId);

        return ResponseEntity.noContent().build();
    }
}
