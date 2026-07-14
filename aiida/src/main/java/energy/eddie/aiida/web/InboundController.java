// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.datasource.mqtt.inbound.ProvisioningTypePatchDto;
import energy.eddie.aiida.dtos.inbound.ProvisioningConnectionDto;
import energy.eddie.aiida.dtos.record.InboundRecordDto;
import energy.eddie.aiida.errors.SecretLoadingException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.inbound.ProvisioningTypeNotConfiguredException;
import energy.eddie.aiida.errors.permission.InvalidInboundPermissionException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.UnsupportedInboundRecordTransformationException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import energy.eddie.aiida.services.InboundProvisioningService;
import energy.eddie.aiida.services.record.InboundRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/inbound")
@Tag(name = "Inbound Controller")
public class InboundController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundController.class);

    private final InboundRecordService inboundRecordService;
    private final InboundProvisioningService inboundProvisioningService;

    public InboundController(
            InboundRecordService inboundRecordService,
            InboundProvisioningService inboundProvisioningService
    ) {
        this.inboundRecordService = inboundRecordService;
        this.inboundProvisioningService = inboundProvisioningService;
    }

    @Operation(summary = "Get latest inbound record for permission")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = InboundRecordDto.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content),
    })
    @GetMapping(value = "/latest/{permissionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InboundRecordDto> latestRecord(
            @PathVariable UUID permissionId,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader,
            @RequestParam(name = "apiKey", required = false) String apiKeyQuery
    ) throws UnauthorizedException, PermissionNotFoundException, InvalidDataSourceTypeException,
             InboundRecordNotFoundException, UnsupportedInboundRecordTransformationException,
             InvalidInboundPermissionException, ProvisioningTypeNotConfiguredException,
             SecretLoadingException {
        String apiKey = (apiKeyHeader != null && !apiKeyHeader.isBlank())
                ? apiKeyHeader
                : apiKeyQuery;

        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("API key missing: provide X-API-Key header or ?apiKey= query param.");
        }

        var inboundRecord = inboundRecordService.latestRecord(permissionId, apiKey);
        return ResponseEntity.ok(inboundRecord);
    }

    @Operation(summary = "Patch provisioning type for permission and activate given inbound retrieval method.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ProvisioningConnectionDto.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content),
    })
    @PatchMapping(value = "permission/{id}/patchInboundProvisioning")
    public ResponseEntity<ProvisioningConnectionDto> patchInboundProvisioningType(
            @PathVariable("id") UUID permissionId,
            @RequestBody ProvisioningTypePatchDto provisioningTypePatchDto
    ) throws PermissionNotFoundException, InvalidDataSourceTypeException {
        LOGGER.info("Fetching latest inbound permission record for permission with ID: {}", permissionId);

        var dto = inboundProvisioningService.changeInboundProvisioningType(provisioningTypePatchDto,
                                                                           permissionId);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Get all provisioning types", description = "Retrieve all provisioning types.",
            operationId = "getProvisioningTypes", tags = {"provisioningType"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Map.class))))
    })
    @GetMapping("/provisioningTypes")
    public ResponseEntity<Map<String, InboundProvisioningType[]>> getProvisioningTypes() {
        return ResponseEntity.ok(Map.of("provisioningTypes", InboundProvisioningType.values()));
    }
}
