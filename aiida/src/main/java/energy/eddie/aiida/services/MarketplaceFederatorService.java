package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.FailedToCreateCSRException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;

@Service
public class MarketplaceFederatorService {
    private static final int RSA_KEY_LENGTH = 2048;
    private static final String FEDERATOR_URL = "http://localhost:9090/api/certificates";
    private static final String BC_PROVIDER = "BC";

    private final WebClient webClient;
    private final KeyStoreService keyStoreService;

    public MarketplaceFederatorService(WebClient.Builder webClientBuilder, KeyStoreService keyStoreService) {
        Security.addProvider(new BouncyCastleProvider());
        this.webClient = webClientBuilder.baseUrl(FEDERATOR_URL).build();
        this.keyStoreService = keyStoreService;
    }

    public X509Certificate getRootCACertificate() throws IOException, CertificateException {
        String pemCert = webClient.get()
                                  .uri("/ca")
                                  .retrieve()
                                  .bodyToMono(String.class)
                                  .block();

        return parseCertificate(pemCert);
    }

    public UUID getFederatorAiidaId() throws FailedToCreateCSRException {
        return UUID.randomUUID();
    }

    public String generateAndRequestCertificate() throws NoSuchAlgorithmException, OperatorCreationException, NoSuchProviderException, CertificateException, IOException {
        var keyPair = generateKeyPair();
        var csr = createCSR(getFederatorAiidaId(), keyPair);
        var signedCertificatePem = sendCertificateForSigning(getCSRPem(csr));

        keyStoreService.saveKeyPairAndCertificate(keyPair.getPrivate(), parseCertificate(signedCertificatePem));
        return signedCertificatePem;
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", BC_PROVIDER);
        keyPairGenerator.initialize(RSA_KEY_LENGTH);
        return keyPairGenerator.generateKeyPair();
    }

    private PKCS10CertificationRequest createCSR(
            UUID federatorAiidaId,
            KeyPair keyPair
    ) throws OperatorCreationException {
        X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, federatorAiidaId.toString())
                .addRDN(BCStyle.O, "EDDIE")
                .addRDN(BCStyle.OU, "AIIDA")
                .build();

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BC_PROVIDER) // BouncyCastle
                .build(keyPair.getPrivate());

        return new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic()).build(signer);
    }

    private String getCSRPem(PKCS10CertificationRequest csr) {
        try {
            StringWriter writer = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
            pemWriter.writeObject(csr);
            pemWriter.close();

            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String sendCertificateForSigning(String csrPem) {
        try {
            String signedCertificatePem = webClient.post()
                                                   .uri("/request")
                                                   .contentType(MediaType.TEXT_PLAIN)
                                                   .bodyValue(csrPem)
                                                   .retrieve().onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(errorMessage -> {
                                        if (response.statusCode() == HttpStatus.CONFLICT) {
                                            throw new FailedToCreateCSRException(errorMessage);
                                        }
                                        throw new RuntimeException("Unexpected error: " + errorMessage);
                                    })
                    )
                                                   .bodyToMono(String.class)
                                                   .block();

            return signedCertificatePem;
        } catch (Exception e) {
            throw new RuntimeException("Error while sending CSR to Federator for signing", e);
        }
    }

    private X509Certificate parseCertificate(String pemCert) throws IOException, CertificateException {
        PEMParser pemParser = new PEMParser(new StringReader(pemCert));
        X509CertificateHolder certificateHolder = (X509CertificateHolder) pemParser.readObject();
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                                                   .generateCertificate(new ByteArrayInputStream(certificateHolder.getEncoded()));
    }
}