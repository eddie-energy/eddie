package energy.eddie.regionconnector.cds.web;

import energy.eddie.regionconnector.cds.client.CdsApiClientFactory;
import energy.eddie.regionconnector.cds.client.responses.*;
import energy.eddie.regionconnector.cds.dtos.CdsServerCreation;
import energy.eddie.regionconnector.cds.dtos.CdsServerCreationError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CdsController {
    private final CdsApiClientFactory cdsApiClientFactory;

    public CdsController(CdsApiClientFactory cdsApiClientFactory) {this.cdsApiClientFactory = cdsApiClientFactory;}

    @PostMapping("register")
    public ResponseEntity<CdsServerCreationError> registerCdsServer(@RequestBody CdsServerCreation cdsServerCreation) {
        return cdsApiClientFactory.getCdsApiClient(cdsServerCreation.cdsServerUri())
                                  .map(CdsController::mapCdsClientResponse)
                                  .block();
    }

    private static ResponseEntity<CdsServerCreationError> mapCdsClientResponse(ApiClientCreationResponse res) {
        return switch (res) {
            case CreatedApiClientResponse ignored -> ResponseEntity.ok().build();
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
