package energy.eddie.regionconnector.cds.web;

import energy.eddie.regionconnector.cds.client.admin.AdminClientFactory;
import energy.eddie.regionconnector.cds.client.admin.responses.*;
import energy.eddie.regionconnector.cds.dtos.CdsServerCreation;
import energy.eddie.regionconnector.cds.dtos.CdsServerCreationError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CdsController {
    private final AdminClientFactory adminClientFactory;

    public CdsController(AdminClientFactory adminClientFactory) {this.adminClientFactory = adminClientFactory;}

    @PostMapping("register")
    public ResponseEntity<CdsServerCreationError> registerCdsServer(@RequestBody CdsServerCreation cdsServerCreation) {
        return adminClientFactory.getOrCreate(cdsServerCreation.cdsServerUri())
                                 .map(CdsController::mapCdsClientResponse)
                                 .block();
    }

    private static ResponseEntity<CdsServerCreationError> mapCdsClientResponse(ApiClientCreationResponse res) {
        return switch (res) {
            case CreatedAdminClientResponse ignored -> ResponseEntity.status(HttpStatus.CREATED).build();
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
        };
    }
}
