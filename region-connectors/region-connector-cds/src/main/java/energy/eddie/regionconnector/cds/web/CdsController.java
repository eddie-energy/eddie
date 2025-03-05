package energy.eddie.regionconnector.cds.web;

import energy.eddie.regionconnector.cds.dtos.CdsServerCreation;
import energy.eddie.regionconnector.cds.dtos.CdsServerCreationError;
import energy.eddie.regionconnector.cds.services.client.creation.CdsClientCreationService;
import energy.eddie.regionconnector.cds.services.client.creation.responses.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
            case AuthorizationCodeGrantTypeNotSupported ignored -> ResponseEntity
                    .badRequest()
                    .body(new CdsServerCreationError("Authorization code grant type not supported"));
            case CoverageNotSupportedResponse ignored -> ResponseEntity
                    .badRequest()
                    .body(new CdsServerCreationError("Coverage capability not supported"));
            case OAuthNotSupportedResponse ignored -> ResponseEntity
                    .badRequest()
                    .body(new CdsServerCreationError("OAuth capability not supported"));
            case RefreshTokenGrantTypeNotSupported ignored -> ResponseEntity
                    .badRequest()
                    .body(new CdsServerCreationError("Refresh token grant type not supported"));
            case NotACdsServerResponse ignored -> ResponseEntity
                    .badRequest()
                    .body(new CdsServerCreationError("Not a CDS server"));
            case NoTokenEndpoint ignored -> ResponseEntity
                    .badRequest()
                    .body(new CdsServerCreationError("No token endpoint"));
        };
    }
}
