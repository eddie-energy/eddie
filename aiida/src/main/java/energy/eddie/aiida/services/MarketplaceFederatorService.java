package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.FailedToCreateCSRException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Base64;
import java.util.UUID;

@Service
public class MarketplaceFederatorService {
    public static final int RSA_KEY_LENGTH = 2048;
    private final WebClient webClient;

    public MarketplaceFederatorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:9090/api/certificates").build();
        Security.addProvider(new BouncyCastleProvider());
    }

    public UUID getFederatorAiidaId() throws FailedToCreateCSRException {
        return UUID.randomUUID();
    }

    public String getNewCSR() {
        try {
            return convertCSRToPEM(createCSR(getFederatorAiidaId()));
        } catch (NoSuchAlgorithmException | OperatorCreationException | NoSuchProviderException exception) {
            throw new FailedToCreateCSRException();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PKCS10CertificationRequest createCSR(UUID federatorAiidaId) throws OperatorCreationException, NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(RSA_KEY_LENGTH);
        var keyPair = keyPairGenerator.generateKeyPair();

        X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, federatorAiidaId.toString())
                .addRDN(BCStyle.O, "EDDIE")
                .addRDN(BCStyle.OU, "AIIDA")
                .build();

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC") // BouncyCastle
                .build(keyPair.getPrivate());

        return new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic()).build(signer);
    }

    public String convertCSRToPEM(PKCS10CertificationRequest csr) throws IOException {
        String base64Encoded = Base64.getEncoder().encodeToString(csr.getEncoded());
        return "-----BEGIN CERTIFICATE REQUEST-----\n" +
               base64Encoded.replaceAll("(.{64})", "$1\n") +
               "\n-----END CERTIFICATE REQUEST-----";
    }

    public String requestCertificateSigning(String csrPem) {
        try {
            String signedCertificate = webClient.post()
                    .uri("/request")
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValue(csrPem)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return signedCertificate;
        } catch (Exception e) {
            throw new RuntimeException("Error while sending CSR to Federator for signing", e);
        }
    }
}