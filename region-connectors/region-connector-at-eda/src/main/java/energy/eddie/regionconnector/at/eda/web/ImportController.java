// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestToImport;
import energy.eddie.regionconnector.at.eda.services.ImportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.connectionStatusMessagesStreamFor;

@RestController
@RequestMapping(path = "${eddie.management.server.urlprefix}")
public class ImportController {
    private final ImportService importService;

    public ImportController(ImportService importService) {this.importService = importService;}

    @PostMapping("/permission-request/import")
    public ResponseEntity<CreatedPermissionRequest> importPermissionRequest(@RequestBody @Valid PermissionRequestToImport request) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var createdRequest = importService.importPermissionRequest(request);
        var location = connectionStatusMessagesStreamFor(createdRequest.permissionIds());
        return ResponseEntity.created(location).body(createdRequest);
    }
}
