// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.web;

import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.cds.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.cds.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.cds.exceptions.UnknownPermissionAdministratorException;
import energy.eddie.regionconnector.cds.services.PermissionRequestCreationService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.connectionStatusMessagesStreamFor;

@Controller
public class PermissionRequestController {
    private final PermissionRequestCreationService creationService;

    public PermissionRequestController(PermissionRequestCreationService creationService) {
        this.creationService = creationService;
    }

    @PostMapping(PATH_PERMISSION_REQUEST)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(@RequestBody PermissionRequestForCreation creationDto) throws DataNeedNotFoundException, UnknownPermissionAdministratorException, UnsupportedDataNeedException {
        var pr = creationService.createPermissionRequest(creationDto);
        var location = connectionStatusMessagesStreamFor(pr.permissionId());
        return ResponseEntity.created(location).body(pr);
    }
}
