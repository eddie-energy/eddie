// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.record.LastMessageEventDto;
import energy.eddie.aiida.dtos.record.LatestDataSourceRecordDto;
import energy.eddie.aiida.dtos.record.LatestInboundPermissionRecordDto;
import energy.eddie.aiida.dtos.record.LatestOutboundPermissionRecordDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.InvalidInboundPermissionException;
import energy.eddie.aiida.errors.permission.LatestPermissionRecordNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.LatestAiidaRecordNotFoundException;
import energy.eddie.aiida.errors.record.UnsupportedInboundRecordTransformationException;
import energy.eddie.aiida.services.AuthService;
import energy.eddie.aiida.services.record.LatestRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@RestController
@RequestMapping("/messages")
@Tag(name = "Latest Record Controller")
public class LatestRecordController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatestRecordController.class);
    private final LatestRecordService latestRecordService;
    private final EventStream eventStream;
    private final AuthService authService;

    @Autowired
    public LatestRecordController(LatestRecordService service, EventStream eventStream, AuthService authService) {
        this.latestRecordService = service;
        this.eventStream = eventStream;
        this.authService = authService;
    }

    @Operation(summary = "Gets the latest data source record for a given datasource ID",
            operationId = "latestDataSourceRecord",
            tags = {"dataSourceRecord"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest data source record found",
                    content = @Content(schema = @Schema(implementation = LatestDataSourceRecordDto.class))),
            @ApiResponse(responseCode = "404", description = "Datasource not found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping(value = "data-source/{id}/latest")
    public LatestDataSourceRecordDto latestDataSourceRecord(@PathVariable("id") UUID dataSourceId)
            throws LatestAiidaRecordNotFoundException {
        LOGGER.info("Fetching latest data source record for datasource with ID: {}", dataSourceId);

        return latestRecordService.latestDataSourceRecord(dataSourceId);
    }

    @Operation(summary = "Gets the latest data source records for a given datasource ID",
            operationId = "latestDataSourceRecords",
            tags = {"dataSourceRecord"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "No latest data source records found",
                    content = @Content(schema = @Schema(implementation = LatestDataSourceRecordDto.class))),
            @ApiResponse(responseCode = "404", description = "Datasource not found",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping(value = "data-source/{id}/latestRecords")
    public List<LatestDataSourceRecordDto> latestDataSourceRecords(
            @PathVariable("id") UUID dataSourceId,
            @Parameter(
                    description = "Maximum number of latest records to return"
            )
            @RequestParam("amount") int amount
    )
            throws LatestAiidaRecordNotFoundException {
        LOGGER.info("Fetching latest {} data source records for datasource with ID: {}", amount, dataSourceId);

        return latestRecordService.latestDataSourceRecords(dataSourceId, amount);
    }

    @GetMapping(value = "permission/{id}/outbound/latest")
    public LatestOutboundPermissionRecordDto latestOutboundPermissionRecord(@PathVariable("id") UUID permissionId)
            throws LatestPermissionRecordNotFoundException {
        LOGGER.debug("Fetching latest outbound permission record for permission with ID: {}", permissionId);

        return latestRecordService.latestOutboundPermissionRecord(permissionId);
    }

    @GetMapping(value = "permission/{id}/inbound/latest")
    public LatestInboundPermissionRecordDto latestInboundPermissionRecord(
            @PathVariable("id") UUID permissionId
    ) throws PermissionNotFoundException, InvalidDataSourceTypeException,
             InboundRecordNotFoundException, UnsupportedInboundRecordTransformationException,
             InvalidInboundPermissionException {
        LOGGER.debug("Fetching latest inbound permission record for permission with ID: {}", permissionId);

        return latestRecordService.latestInboundPermissionRecord(permissionId);
    }

    @GetMapping(value = "last-message-stream", produces = TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<LastMessageEventDto>> lastMessageStream() throws InvalidUserException {
        LOGGER.debug("Opening last-message SSE stream for user {}", authService.getCurrentUserId());

        return eventStream.toJson(latestRecordService.lastMessageStream());
    }
}