// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.web;

import energy.eddie.api.agnostic.aiida.AiidaPermissionAcceptDto;
import energy.eddie.api.agnostic.aiida.AiidaPermissionDeclineDto;
import energy.eddie.api.agnostic.aiida.AiidaPermissionRequestsDto;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.aiida.dtos.PermissionDetailsDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import energy.eddie.regionconnector.aiida.exceptions.DataNeedMalformedException;
import energy.eddie.regionconnector.aiida.services.AiidaPermissionService;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.connectionStatusMessagesStreamFor;

@RestController
public class PermissionRequestController {
    public static final String PATH_HANDSHAKE_PERMISSION_REQUEST = PATH_PERMISSION_REQUEST + "/{permissionId}";
    public static final String PATH_HANDSHAKE_ACCEPT = PATH_HANDSHAKE_PERMISSION_REQUEST + "/accept";
    public static final String PATH_HANDSHAKE_REJECT = PATH_HANDSHAKE_PERMISSION_REQUEST + "/reject";
    public static final String PATH_HANDSHAKE_UNFULFILLABLE = PATH_HANDSHAKE_PERMISSION_REQUEST + "/unfulfillable";
    private final AiidaPermissionService permissionService;

    @Autowired
    public PermissionRequestController(AiidaPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping(value = PATH_PERMISSION_REQUEST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AiidaPermissionRequestsDto> createPermissionRequests(
            @Valid @RequestBody PermissionRequestForCreation permissionRequestsForCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, DataNeedMalformedException, JwtCreationFailedException {
        var qrCodeDto = permissionService.createValidateAndSendPermissionRequests(permissionRequestsForCreation);
        var location = connectionStatusMessagesStreamFor(qrCodeDto.permissionIds().toArray());
        return ResponseEntity.created(location).body(qrCodeDto);
    }

    /**
     * Called by AIIDA when the user accepted the permission request. Returns the MQTT credentials and topics AIIDA has
     * to use for this permission.
     */
    @PostMapping(value = PATH_HANDSHAKE_ACCEPT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MqttDto> acceptPermissionRequest(
            @PathVariable String permissionId,
            @Valid @RequestBody AiidaPermissionAcceptDto acceptDto
    ) throws PermissionNotFoundException, CredentialsAlreadyExistException, PermissionStateTransitionException, DataNeedNotFoundException {
        return ResponseEntity.ok(permissionService.acceptPermission(permissionId, acceptDto.aiidaId()));
    }

    /**
     * Called by AIIDA when the user rejected the permission request.
     */
    @PostMapping(value = PATH_HANDSHAKE_REJECT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> rejectPermissionRequest(
            @PathVariable String permissionId,
            @Valid @RequestBody AiidaPermissionDeclineDto declineDto
    ) throws PermissionNotFoundException, PermissionStateTransitionException {
        permissionService.rejectPermission(permissionId, declineDto.aiidaId(), declineDto.reason());
        return ResponseEntity.noContent().build();
    }

    /**
     * Called by AIIDA when the AIIDA instance cannot fulfill the permission request, e.g. because the requested data is
     * not available.
     */
    @PostMapping(value = PATH_HANDSHAKE_UNFULFILLABLE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> markPermissionRequestUnfulfillable(
            @PathVariable String permissionId,
            @Valid @RequestBody AiidaPermissionDeclineDto declineDto
    ) throws PermissionNotFoundException, PermissionStateTransitionException {
        permissionService.unableToFulfillPermission(permissionId, declineDto.aiidaId(), declineDto.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = PATH_HANDSHAKE_PERMISSION_REQUEST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PermissionDetailsDto> getPermissionDetails(@PathVariable String permissionId) throws DataNeedNotFoundException, PermissionNotFoundException {
        return ResponseEntity.ok(permissionService.detailsForPermission(permissionId));
    }
}
