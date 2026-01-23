// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.web;

import energy.eddie.regionconnector.cds.dtos.CdsServerCreation;
import energy.eddie.regionconnector.cds.dtos.CdsServerCreationError;
import energy.eddie.regionconnector.cds.services.client.creation.CdsClientCreationService;
import energy.eddie.regionconnector.cds.services.client.creation.responses.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "${eddie.management.server.urlprefix}")
public class CdsController {
    private final CdsClientCreationService clientCreationService;

    public CdsController(CdsClientCreationService clientCreationService) {this.clientCreationService = clientCreationService;}

    @PostMapping("register")
    public ResponseEntity<CdsServerCreationError> registerCdsServer(@RequestBody CdsServerCreation cdsServerCreation) {
        var creationResponse = clientCreationService.createOAuthClients(cdsServerCreation.cdsServerUri());
        return toResponseEntity(creationResponse);
    }

    private static ResponseEntity<CdsServerCreationError> toResponseEntity(ApiClientCreationResponse res) {
        return switch (res) {
            case CreatedCdsClientResponse ignored -> ResponseEntity.status(HttpStatus.CREATED).build();
            case NotACdsServerResponse ignored -> ResponseEntity
                    .badRequest()
                    .body(new CdsServerCreationError("Not a CDS server"));
            case UnsupportedFeatureResponse(String message) -> ResponseEntity
                    .badRequest()
                    .body(new CdsServerCreationError(message));
            case UnableToRegisterClientResponse(String message) -> ResponseEntity
                    .badRequest()
                    .body(new CdsServerCreationError(message));
        };
    }
}
