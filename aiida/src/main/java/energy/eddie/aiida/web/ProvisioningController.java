// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.provisioning.MqttProvisioningConnectionDto;
import energy.eddie.aiida.dtos.provisioning.ProvisioningTypePatchDto;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
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

    /**
     * Creates a controller for configuring and discovering inbound provisioning types.
     *
     * @param provisioningService Service that applies provisioning changes.
     */
    public ProvisioningController(
            ProvisioningService provisioningService
    ) {
        this.provisioningService = provisioningService;
    }

    /**
     * Changes the provisioning type for the inbound data source associated with a permission.
     *
     * @param permissionId             ID of the permission to reconfigure.
     * @param provisioningTypePatchDto Requested provisioning type and connection parameters.
     * @return The resulting MQTT connection details, or an empty connection DTO for REST provisioning.
     * @throws PermissionNotFoundException    If no permission exists for {@code permissionId}.
     * @throws InvalidDataSourceTypeException If the permission is not associated with an inbound data source.
     */
    @Operation(summary = "Patch provisioning type for permission and activate given inbound retrieval method.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = MqttProvisioningConnectionDto.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content),
    })
    @PatchMapping(value = "/permission/{id}/patchInboundProvisioning")
    public ResponseEntity<MqttProvisioningConnectionDto> patchInboundProvisioningType(
            @PathVariable("id") UUID permissionId,
            @RequestBody ProvisioningTypePatchDto provisioningTypePatchDto
    ) throws PermissionNotFoundException, InvalidDataSourceTypeException {
        LOGGER.info("Fetching latest inbound permission record for permission with ID: {}", permissionId);

        var dto = provisioningService.changeProvisioningType(permissionId, provisioningTypePatchDto);
        return ResponseEntity.ok(dto);
    }

    /**
     * Returns every provisioning type supported for inbound data.
     *
     * @return A response containing all supported provisioning types under the {@code provisioningTypes} key.
     */
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
