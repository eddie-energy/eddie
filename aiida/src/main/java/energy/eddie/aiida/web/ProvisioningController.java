// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.provisioning.MqttProvisioningConnectionDto;
import energy.eddie.aiida.dtos.provisioning.ProvisioningTypePatchDto;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.inbound.ProvisioningConfigurationException;
import energy.eddie.aiida.errors.inbound.ProvisioningTypeNotConfiguredException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import energy.eddie.aiida.services.ProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/provisioning")
@Tag(name = "Provisioning Controller")
public class ProvisioningController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningController.class);

    private final ProvisioningService provisioningService;

    public ProvisioningController(
            ProvisioningService provisioningService
    ) {
        this.provisioningService = provisioningService;
    }

    @Operation(summary = "Patch provisioning type for permission and activate given inbound retrieval method.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = MqttProvisioningConnectionDto.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content),
    })
    @PatchMapping(value = "/permission/{id}/patchInboundProvisioning")
    public ResponseEntity<MqttProvisioningConnectionDto> patchInboundProvisioningType(
            @PathVariable("id") UUID permissionId,
            @Valid @RequestBody ProvisioningTypePatchDto provisioningTypePatchDto
    ) throws PermissionNotFoundException,
             InvalidDataSourceTypeException,
             ProvisioningConfigurationException {
        LOGGER.info("Fetching latest inbound permission record for permission with ID: {}", permissionId);

        var dto = provisioningService.changeProvisioningType(permissionId, provisioningTypePatchDto);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Reset the password for MQTT server provisioning.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(schema = @Schema(implementation = MqttProvisioningConnectionDto.class))),
            @ApiResponse(responseCode = "400", description = "Permission does not use an inbound data source",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Permission not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "MQTT server provisioning is not active",
                    content = @Content)
    })
    @PostMapping(value = "/permission/{id}/regenerate-server-provisioning-password")
    public ResponseEntity<MqttProvisioningConnectionDto> resetServerModePassword(
            @PathVariable("id") UUID permissionId
    ) throws PermissionNotFoundException,
             InvalidDataSourceTypeException,
             ProvisioningTypeNotConfiguredException,
             ProvisioningConfigurationException {
        var connectionDetails = provisioningService.resetServerModePassword(permissionId);
        return ResponseEntity.ok(connectionDetails);
    }

    @Operation(summary = "Get all provisioning types", description = "Retrieve all provisioning types.",
            operationId = "getProvisioningTypes", tags = {"provisioningType"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Map.class))))
    })
    @GetMapping("/types")
    public ResponseEntity<Map<String, InboundProvisioningType[]>> getProvisioningTypes() {
        return ResponseEntity.ok(Map.of("provisioningTypes", InboundProvisioningType.values()));
    }
}
