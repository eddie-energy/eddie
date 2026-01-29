// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.web;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.be.fluvius.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.be.fluvius.service.AcceptanceOrRejectionService;
import energy.eddie.regionconnector.be.fluvius.service.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.*;

@Controller
public class PermissionRequestController {
    private static final String STATUS = "status";
    private final PermissionRequestService permissionRequestService;
    private final AcceptanceOrRejectionService acceptanceOrRejectionService;

    public PermissionRequestController(
            PermissionRequestService permissionRequestService,
            AcceptanceOrRejectionService acceptanceOrRejectionService
    ) {
        this.permissionRequestService = permissionRequestService;
        this.acceptanceOrRejectionService = acceptanceOrRejectionService;
    }

    @PostMapping(
            value = PATH_PERMISSION_REQUEST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(@Valid @RequestBody PermissionRequestForCreation permissionRequestForCreation) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionRequest = permissionRequestService.createPermissionRequest(permissionRequestForCreation);
        return ResponseEntity.created(new UriTemplate(CONNECTION_STATUS_STREAM).expand(permissionRequest.permissionId()))
                             .body(permissionRequest);
    }

    @GetMapping(PATH_PERMISSION_ACCEPTED)
    public String callbackSuccess(@PathVariable String permissionId, Model model) {
        return handlePermissionCallback(permissionId, model, PermissionProcessStatus.ACCEPTED);
    }

    @GetMapping(PATH_PERMISSION_REJECTED)
    public String callbackRejected(@PathVariable String permissionId, Model model) {
        return handlePermissionCallback(permissionId, model, PermissionProcessStatus.REJECTED);
    }

    private String handlePermissionCallback(String permissionId, Model model, PermissionProcessStatus status) {
        try {
            var wasAccepted = acceptanceOrRejectionService.acceptOrRejectPermissionRequest(permissionId, status);
            if (wasAccepted) {
                model.addAttribute(STATUS, "OK");
            } else {
                model.addAttribute(STATUS, "DENIED");
            }
        } catch (PermissionNotFoundException e) {
            model.addAttribute(STATUS, "ERROR");
        }
        return "authorization-callback";
    }
}
