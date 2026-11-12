// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.dtos.monitoring.MeasurementPointDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.connectionlimit.ConnectionLimitMonitoringNotAllowedException;
import energy.eddie.aiida.errors.conversion.InvalidInstantOrDurationException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitViolation;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitService;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitViolationService;
import energy.eddie.aiida.services.monitoring.MeasurementService;
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
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/connection-limits")
@Tag(name = "Connection Limit Controller")
public class ConnectionLimitController {
    private static final Duration DEFAULT_MEASUREMENT_RANGE = Duration.ofDays(1);

    private final ConnectionLimitService connectionLimitService;
    private final ConnectionLimitViolationService connectionLimitViolationService;
    private final MeasurementService measurementService;
    private final Clock clock;

    public ConnectionLimitController(
            ConnectionLimitService connectionLimitService,
            ConnectionLimitViolationService connectionLimitViolationService,
            MeasurementService measurementService,
            Clock clock
    ) {
        this.connectionLimitService = connectionLimitService;
        this.connectionLimitViolationService = connectionLimitViolationService;
        this.measurementService = measurementService;
        this.clock = clock;
    }

    @Operation(
            summary = "Get connection limits",
            description = """
                    If no time frame is provided, returns all connection limits that apply NOW.
                    Limits can be filtered by permission ID, meter ID, and time frame.
                    Overlapping limits are resolved to one effective timeline per permission and meter.
                    The most recently added limit is chosen as the effective limit.
                    No checks are made if the permission actually exists and can yield connection limits!
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConnectionLimitDto.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(
                            schema = @Schema(implementation = EddieApiError.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "errors": [
                                        {
                                          "message": "Could not parse Instant or Duration from 'foo'"
                                        }
                                      ]
                                    }
                                    """)
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
            @Parameter(description = "Lower bound of the search interval (inclusive), UTC instant or ISO duration.", example = "2026-07-10T08:00:00Z", examples = {@ExampleObject("2026-07-10T08:00:00Z"), @ExampleObject("-P1D")})
            @RequestParam(required = false) String from,
            @Parameter(description = "Upper bound of the search interval (inclusive), UTC instant or ISO duration.", example = "P1D", examples = {@ExampleObject("2026-07-12T08:00:00Z"), @ExampleObject("P1D")})
            @RequestParam(required = false) String to
    ) throws InvalidUserException, InvalidInstantOrDurationException {
        return ResponseEntity.ok(connectionLimitService.getConnectionLimits(permissionId,
                                                                            meterId,
                                                                            fromInstantOrDuration(from),
                                                                            fromInstantOrDuration(to)));
    }

    @Operation(summary = "Get connection limit violations of a permission", description = """
            Returns the violations of the connection limits of the permission that overlap with the time frame,
            ordered by their start. If no time frame is provided, violations that are ongoing NOW are returned.
            A violation without an end is still ongoing.
            The limits of a violation are only the ones that were violated first.
            """)
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
                                        description = "Successful operation",
                                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConnectionLimitViolation.class)))), @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = EddieApiError.class))), @ApiResponse(responseCode = "401",
                                                                                                      description = "Unauthorized User",
                                                                                                      content = @Content), @ApiResponse(
            responseCode = "404",
            description = "Permission not found",
            content = @Content(schema = @Schema(implementation = EddieApiError.class)))})
    @GetMapping(path = "/{permissionId}/violations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ConnectionLimitViolation>> getViolations(
            @Parameter(description = "Permission ID to query violations for.",
                       example = "9921f327-f341-4bea-bf08-3cf2acc65bf3") @PathVariable UUID permissionId,
            @Parameter(description = "Lower bound of the search interval (inclusive), UTC instant or ISO duration.",
                       example = "2026-07-10T08:00:00Z",
                       examples = {@ExampleObject("2026-07-10T08:00:00Z"), @ExampleObject("-P1D")}) @RequestParam(
                    required = false) String from,
            @Parameter(description = "Upper bound of the search interval (inclusive), UTC instant or ISO duration.",
                       example = "P1D",
                       examples = {@ExampleObject("2026-07-12T08:00:00Z"), @ExampleObject("P1D")}) @RequestParam(
                    required = false) String to
    ) throws InvalidUserException, InvalidInstantOrDurationException, PermissionNotFoundException {
        return ResponseEntity.ok(connectionLimitViolationService.getViolations(permissionId,
                                                                               fromInstantOrDuration(from),
                                                                               fromInstantOrDuration(to)));
    }

    @Operation(
            summary = "Get measurements of a connection limit permission",
            description = """
                    Returns the measured signed power in kW of the data source that is monitored for the permission,
                    ordered by timestamp. Imported power is positive, exported power is negative.
                    One point is returned per retained record, without aggregation.
                    Measurements are only available as long as the records of the data source are retained.
                    If no time frame is provided, the measurements of the last day are returned.
                    If no data source is monitored for the permission, an empty list is returned.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MeasurementPointDto.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or permission does not support connection limits", content = @Content(schema = @Schema(implementation = EddieApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized User", content = @Content),
            @ApiResponse(responseCode = "404", description = "Permission not found", content = @Content(schema = @Schema(implementation = EddieApiError.class)))
    })
    @GetMapping(path = "/{permissionId}/measurements", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MeasurementPointDto>> getMeasurements(
            @Parameter(description = "Permission ID to query measurements for.",
                       example = "9921f327-f341-4bea-bf08-3cf2acc65bf3")
            @PathVariable UUID permissionId,
            @Parameter(description = "Lower bound of the search interval (inclusive), UTC instant or ISO duration.", example = "2026-07-10T08:00:00Z", examples = {@ExampleObject("2026-07-10T08:00:00Z"), @ExampleObject("-P1D")})
            @RequestParam(required = false) String from,
            @Parameter(description = "Upper bound of the search interval (inclusive), UTC instant or ISO duration.", example = "2026-07-11T08:00:00Z", examples = {@ExampleObject("2026-07-11T08:00:00Z"), @ExampleObject("P1D")})
            @RequestParam(required = false) String to
    ) throws InvalidUserException, InvalidInstantOrDurationException, PermissionNotFoundException,
             ConnectionLimitMonitoringNotAllowedException {
        return ResponseEntity.ok(measurementService.getMeasurements(
                permissionId,
                fromInstantOrDuration(from, DEFAULT_MEASUREMENT_RANGE.negated()),
                fromInstantOrDuration(to)));
    }

    private Instant fromInstantOrDuration(@Nullable String value) throws InvalidInstantOrDurationException {
        return fromInstantOrDuration(value, Duration.ZERO);
    }

    private Instant fromInstantOrDuration(
            @Nullable String value,
            Duration defaultOffset
    ) throws InvalidInstantOrDurationException {
        if (value == null || value.isBlank()) {
            return clock.instant().plus(defaultOffset);
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return clock.instant().plus(Duration.parse(value));
            } catch (DateTimeParseException e) {
                throw new InvalidInstantOrDurationException(value);
            }
        }
    }
}
