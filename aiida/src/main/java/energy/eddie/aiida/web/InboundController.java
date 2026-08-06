// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

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
import energy.eddie.aiida.services.record.InboundRecordService;
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

    private final InboundRecordService inboundRecordService;

    /**
     * Creates the controller used to retrieve inbound records through REST provisioning.
     *
     * @param inboundRecordService Service that retrieves and authorizes inbound records.
     */
    public InboundController(
            InboundRecordService inboundRecordService
    ) {
        this.inboundRecordService = inboundRecordService;
    }

    /**
     * Returns the latest inbound record using the configured REST provisioning mechanism. A non-blank
     * {@code X-API-Key} header selects bearer provisioning; otherwise a non-blank {@code apiKey} query parameter
     * selects API-token provisioning.
     *
     * @param permissionId ID of the inbound permission whose latest record should be returned.
     * @param bearerKey    Access code supplied through the {@code X-API-Key} header.
     * @param queryKey     Access code supplied through the {@code apiKey} query parameter.
     * @return A response containing the latest inbound record.
     * @throws PermissionNotFoundException                     If the permission does not exist.
     * @throws UnauthorizedException                           If no access code is supplied or the code is invalid.
     * @throws InvalidDataSourceTypeException                  If the permission does not use an inbound data source.
     * @throws InboundRecordNotFoundException                  If no inbound record exists for the data source.
     * @throws UnsupportedInboundRecordTransformationException If the record cannot be transformed to the configured format.
     * @throws InvalidInboundPermissionException               If the permission has no inbound message format.
     * @throws ProvisioningTypeNotConfiguredException          If the selected REST provisioning type is not active.
     * @throws SecretLoadingException                          If the stored access code cannot be loaded.
     */
    @GetMapping(
            value = "/latest/{permissionId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<InboundRecordDto> latestRecord(
            @PathVariable UUID permissionId,
            @RequestHeader(value = "X-API-Key", required = false) String bearerKey,
            @RequestParam(value = "apiKey", required = false) String queryKey
    ) throws PermissionNotFoundException,
             UnauthorizedException,
             InvalidDataSourceTypeException,
             InboundRecordNotFoundException,
             UnsupportedInboundRecordTransformationException,
             InvalidInboundPermissionException,
             ProvisioningTypeNotConfiguredException,
             SecretLoadingException {
        if (bearerKey != null && !bearerKey.isBlank()) {
            return ResponseEntity.ok(inboundRecordService.latestRecord(
                    permissionId,
                    bearerKey,
                    InboundProvisioningType.REST_BEARER
            ));
        }

        if (queryKey != null && !queryKey.isBlank()) {
            return ResponseEntity.ok(inboundRecordService.latestRecord(
                    permissionId,
                    queryKey,
                    InboundProvisioningType.REST_API_TOKEN
            ));
        }

        throw new UnauthorizedException(
                "API key missing: provide X-API-Key header or apiKey query parameter."
        );
    }
}
