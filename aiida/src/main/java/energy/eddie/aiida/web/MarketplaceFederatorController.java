package energy.eddie.aiida.web;

import energy.eddie.aiida.services.MarketplaceFederatorService;
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
        var csr = marketplaceFederatorService.getNewCSR();
        var signedCertificate = marketplaceFederatorService.requestCertificateSigning(csr);
        return ResponseEntity.ok(signedCertificate);
    }
}
