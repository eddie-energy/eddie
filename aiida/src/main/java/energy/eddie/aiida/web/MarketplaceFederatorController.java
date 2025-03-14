package energy.eddie.aiida.web;

import energy.eddie.aiida.services.MarketplaceFederatorService;
import org.bouncycastle.operator.OperatorCreationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@RestController
public class MarketplaceFederatorController {

    private final MarketplaceFederatorService marketplaceFederatorService;

    public MarketplaceFederatorController(MarketplaceFederatorService marketplaceFederatorService) {this.marketplaceFederatorService = marketplaceFederatorService;}

    @CrossOrigin(origins = "http://localhost:63342")
    @GetMapping("/request-certificate")
    public ResponseEntity<String> requestCertificate() throws CertificateException, NoSuchAlgorithmException, IOException, OperatorCreationException, NoSuchProviderException {
        var signedCertificate = marketplaceFederatorService.generateAndRequestCertificate();
        return ResponseEntity.ok(signedCertificate);
    }

    @CrossOrigin(origins = "http://localhost:63342")
    @GetMapping("/request-root-ca")
    public ResponseEntity<X509Certificate> validateCertificate() throws CertificateException, IOException {
        var rootCACertificate = marketplaceFederatorService.getRootCACertificate();
        //Todo: Service Methode zum überprüfen.
        return ResponseEntity.ok(rootCACertificate);
    }
}
