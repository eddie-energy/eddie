// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            summary = "Get connection limits",
            description = """
                    Returns all currently active connection limits by default.
                    Limits can be filtered by permission ID, meter ID, and time frame.
                    Overlapping limits are resolved to one effective timeline per permission and meter.
                    The most recently added limit is chosen as the effective limit.
                    An offset can be provided to return the next N limits from now or in the time frame.
                    No checks are made if the permission actually exists and can yield connection limits!
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConnectionLimitDto.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(
                            schema = @Schema(implementation = EddieApiError.class),
                            examples = @ExampleObject(value = "{\"errors\":[{\"message\":\"getConnectionLimits.offset: must be greater than or equal to 0\"}]}")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized User", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ConnectionLimitDto>> getConnectionLimits(
            @Parameter(description = "Permission ID to query limits for.", example = "9921f327-f341-4bea-bf08-3cf2acc65bf3")
            @RequestParam(required = false) UUID permissionId,
            @Parameter(description = "Meter ID to query limits for.", example = "003114735")
            @RequestParam(required = false) String meterId,
            @Parameter(description = "Lower bound of the search interval (inclusive), UTC instant.", example = "2026-07-10T08:00:00Z")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "Upper bound of the search interval (inclusive), UTC instant. Acts as cap when used with offset.", example = "2026-07-12T08:00:00Z")
            @RequestParam(required = false) Instant to,
            @Parameter(description = "Returns current/from plus N next limits. Must be >= 0.", example = "2")
            @RequestParam(required = false) @Min(0) Integer offset
    ) throws InvalidUserException {
        return ResponseEntity.ok(connectionLimitService.getConnectionLimits(permissionId, meterId, from, to, offset));
    }
}
