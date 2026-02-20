// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.record.InboundRecordDto;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.services.InboundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/inbound")
@Tag(name = "Inbound Controller")
public class InboundController {
    private final InboundService inboundService;

    public InboundController(InboundService inboundService) {
        this.inboundService = inboundService;
    }

    @Operation(summary = "Get latest inbound record for permission")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = InboundRecordDto.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content),
    })
    @GetMapping(value = "/latest/{permissionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InboundRecordDto> latestRecord(
            @PathVariable UUID permissionId,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader,
            @RequestParam(name = "apiKey", required = false) String apiKeyQuery
    ) throws UnauthorizedException, PermissionNotFoundException, InvalidDataSourceTypeException, InboundRecordNotFoundException {
        String apiKey = (apiKeyHeader != null && !apiKeyHeader.isBlank())
                ? apiKeyHeader
                : apiKeyQuery;

        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("API key missing: provide X-API-Key header or ?apiKey= query param.");
        }

        var inboundRecord = inboundService.latestRecord(permissionId, apiKey);
        return ResponseEntity.ok(inboundRecord.toDto());
    }
}
