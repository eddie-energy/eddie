// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.record.LatestDataSourceRecordDto;
import energy.eddie.aiida.dtos.record.LatestInboundPermissionRecordDto;
import energy.eddie.aiida.dtos.record.LatestOutboundPermissionRecordDto;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.LatestPermissionRecordNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.LatestAiidaRecordNotFoundException;
import energy.eddie.aiida.services.LatestRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/messages")
@Tag(name = "Latest Record Controller")
public class LatestRecordController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatestRecordController.class);
    private final LatestRecordService latestRecordService;

    @Autowired
    public LatestRecordController(LatestRecordService service) {
        this.latestRecordService = service;
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

    @GetMapping(value = "permission/{id}/outbound/latest")
    public LatestOutboundPermissionRecordDto latestOutboundPermissionRecord(@PathVariable("id") UUID permissionId)
            throws LatestPermissionRecordNotFoundException {
        LOGGER.info("Fetching latest outbound permission record for permission with ID: {}", permissionId);

        return latestRecordService.latestOutboundPermissionRecord(permissionId);
    }

    @GetMapping(value = "permission/{id}/inbound/latest")
    public LatestInboundPermissionRecordDto latestInboundPermissionRecord(
            @PathVariable("id") UUID permissionId
    ) throws PermissionNotFoundException, InvalidDataSourceTypeException, InboundRecordNotFoundException {
        LOGGER.info("Fetching latest inbound permission record for permission with ID: {}", permissionId);

        return latestRecordService.latestInboundPermissionRecord(permissionId);
    }
}