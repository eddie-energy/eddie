// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet.web;

import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.onenet.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.onenet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.onenet.service.PermissionCreationService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.connectionStatusMessagesStreamFor;

@Controller
public class PermissionRequestController {

    private final PermissionCreationService permissionCreationService;

    public PermissionRequestController(PermissionCreationService permissionCreationService) {this.permissionCreationService = permissionCreationService;}

    @PostMapping(PATH_PERMISSION_REQUEST)
    public ResponseEntity<CreatedPermissionRequest> createPermissionRequest(@RequestBody PermissionRequestForCreation creationDto) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var pr = permissionCreationService.createPermissionRequest(creationDto);
        var location = connectionStatusMessagesStreamFor(pr.permissionId());
        return ResponseEntity.created(location).body(pr);
    }
}
