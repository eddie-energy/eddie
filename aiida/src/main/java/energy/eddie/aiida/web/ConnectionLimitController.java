// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.connectionlimit.PermissionDoesNotSupportConnectionLimitsException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitService;
import energy.eddie.api.agnostic.EddieApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/connection-limits")
@Tag(name = "Connection Limit Controller")
public class ConnectionLimitController {
    private final ConnectionLimitService connectionLimitService;

    public ConnectionLimitController(ConnectionLimitService connectionLimitService) {
        this.connectionLimitService = connectionLimitService;
    }

    @Operation(
            summary = "Get connection limits for a permission",
            description = "Returns the connection limits per interval for one permission. Without parameters defaults to the currently active interval(s)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConnectionLimitDto.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(
                            schema = @Schema(implementation = EddieApiError.class),
                            examples = @ExampleObject(value = "{\"errors\":[{\"message\":\"getConnectionLimits.offset: must be greater than or equal to 0\"}]}")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized User", content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "Permission not found",
                    content = @Content(
                            schema = @Schema(implementation = EddieApiError.class),
                            examples = @ExampleObject(value = "{\"errors\":[{\"message\":\"No permission with ID '9921f327-f341-4bea-bf08-3cf2acc65bf3' found.\"}]}")
                    )),
            @ApiResponse(responseCode = "422",
                    description = "Permission does not support connection limits",
                    content = @Content(
                            schema = @Schema(implementation = EddieApiError.class),
                            examples = @ExampleObject(value = "{\"errors\":[{\"message\":\"Permission with ID '9921f327-f341-4bea-bf08-3cf2acc65bf3' does not support connection limits.\"}]}")
                    ))
    })
    @GetMapping(value = "/{permissionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ConnectionLimitDto>> getConnectionLimits(
            @Parameter(description = "Permission ID to query limits for.", example = "9921f327-f341-4bea-bf08-3cf2acc65bf3")
            @PathVariable UUID permissionId,
            @Parameter(description = "Lower bound of the search interval (inclusive), UTC instant.", example = "2026-07-10T08:00:00Z")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "Upper bound of the search interval (inclusive), UTC instant. Acts as cap when used with offset.", example = "2026-07-12T08:00:00Z")
            @RequestParam(required = false) Instant to,
            @Parameter(description = "Returns current/from plus N next limits. Must be >= 0.", example = "2")
            @RequestParam(required = false) @Min(0) Integer offset
    ) throws InvalidUserException, PermissionNotFoundException, PermissionDoesNotSupportConnectionLimitsException {
        return ResponseEntity.ok(connectionLimitService.getConnectionLimits(permissionId, from, to, offset));
    }
}
