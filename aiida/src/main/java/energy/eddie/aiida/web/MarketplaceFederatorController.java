package energy.eddie.aiida.web;

import energy.eddie.aiida.errors.FailedToCreateAiidaFederatorConnectionMessage;
import energy.eddie.aiida.errors.FailedToCreateCSRException;
import energy.eddie.aiida.services.MarketplaceFederatorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarketplaceFederatorController {

    private final MarketplaceFederatorService marketplaceFederatorService;

    public MarketplaceFederatorController(MarketplaceFederatorService marketplaceFederatorService) {this.marketplaceFederatorService = marketplaceFederatorService;}

    @CrossOrigin(origins = "http://localhost:63342")
    @GetMapping("/request-certificate")
    public ResponseEntity<String> requestCertificate() {
        try {
            return ResponseEntity.ok(marketplaceFederatorService.requestAiidaFederatorCertificate());
        } catch (FailedToCreateCSRException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to create CSR: " + e.getMessage());
        }
    }

    @CrossOrigin(origins = "http://localhost:63342")
    @GetMapping("/create-connection-message")
    public ResponseEntity<String> createConnectionMessage() {
        try {
            return ResponseEntity.ok(marketplaceFederatorService.createAiidaFederatorConnectionMessage());
        }  catch (FailedToCreateAiidaFederatorConnectionMessage e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to create Aiida to Federator connection message: " + e.getMessage());
        }
    }
}
