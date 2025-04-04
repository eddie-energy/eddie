package energy.eddie.aiida.services;

import energy.eddie.aiida.config.MarketplaceConfiguration;
import energy.eddie.aiida.errors.FailedToCreateAiidaFederatorConnectionMessage;
import energy.eddie.aiida.errors.FailedToCreateCSRException;
import energy.eddie.aiida.errors.FailedToGetCertificateException;
import energy.eddie.aiida.errors.KeyStoreServiceException;
import energy.eddie.api.agnostic.EddieApiError;
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
import org.bouncycastle.util.io.pem.PemObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.UUID;

@Service
public class MarketplaceFederatorService {
    private static final int RSA_KEY_LENGTH = 2048;
    private static final String BC_PROVIDER = "BC";
    private final WebClient webClient;
    private final KeyStoreService keyStoreService;
    private final UUID aiidaId;

    public MarketplaceFederatorService(
            WebClient.Builder webClientBuilder,
            KeyStoreService keyStoreService,
            MarketplaceConfiguration marketplaceConfiguration,
            ApplicationInformationService applicationInformationService
    ) {
        Security.addProvider(new BouncyCastleProvider());
        this.keyStoreService = keyStoreService;
        this.webClient = webClientBuilder.baseUrl(marketplaceConfiguration.getMarketplaceFederatorUrl()).build();
        this.aiidaId = applicationInformationService.applicationInformation().aiidaId();
    }

    public X509Certificate getRootCACertificate() throws FailedToGetCertificateException {
        try {
            String pemCert = webClient.get()
                                      .uri("/ca")
                                      .retrieve()
                                      .bodyToMono(String.class)
                                      .block();
            return parsePemToCertificate(pemCert);
        } catch (IOException | CertificateException e) {
            throw new FailedToGetCertificateException("Failed to fetch Root CA Certificate from Federator: " + e);
        }
    }

    public String requestAiidaFederatorCertificate() throws FailedToCreateCSRException, FailedToGetCertificateException {
        try {
            if (keyStoreService.getCertificate() != null) {
                return convertCertificateToPem(keyStoreService.getCertificate());
            } else {
                var keyPair = generateKeyPair();
                var csr = createCSR(keyPair);
                var signedCertificatePem = sendCSRForSigning(convertCSRToPem(csr));
                var signedCertificate = parsePemToCertificate(signedCertificatePem);

                verifyCertificate(signedCertificate, keyPair);
                keyStoreService.saveKeyPairAndCertificate(keyPair.getPrivate(), signedCertificate);

                return signedCertificatePem;
            }
        } catch (KeyStoreServiceException | IOException | CertificateException e) {
            throw new FailedToGetCertificateException("Failed to get Certificate for Aiida: " + e);
        }
    }

    public String createAiidaFederatorConnectionMessage() throws FailedToCreateAiidaFederatorConnectionMessage {
        var message = "federatorAiidaId: " + aiidaId;
        try {
            return signMessage(message, keyStoreService.getKeyPair().getPrivate());
        } catch (KeyStoreServiceException e) {
            throw new FailedToCreateAiidaFederatorConnectionMessage("Failed load Private Key", e);
        }
    }

    private String signMessage(
            String message,
            PrivateKey privateKey
    ) throws FailedToCreateAiidaFederatorConnectionMessage {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA", BC_PROVIDER);
            signature.initSign(privateKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));

            byte[] signedData = signature.sign();
            return convertByteArrayToPem(signedData);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException |
                 IOException e) {
            throw new FailedToCreateAiidaFederatorConnectionMessage("Failed to sign connection message", e);
        }
    }

    private void verifyCertificate(
            X509Certificate certificate,
            KeyPair keyPair
    ) throws FailedToCreateCSRException {
        try {
            var rootCACertificate = getRootCACertificate();
            certificate.verify(rootCACertificate.getPublicKey());

            if (!keyPair.getPublic().equals(certificate.getPublicKey())) {
                throw new IllegalArgumentException("Wrong Public Key. This key does not match the public key of AIIDA.");
            }

            Date now = new Date();
            certificate.checkValidity(now);
        } catch (FailedToGetCertificateException | CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
            throw new FailedToCreateCSRException("Error while verifying certificate", e);
        }
    }

    private KeyPair generateKeyPair() throws FailedToCreateCSRException {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", BC_PROVIDER);
            keyPairGenerator.initialize(RSA_KEY_LENGTH);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new FailedToCreateCSRException("Error while creating Keypair", e);
        }
    }

    private PKCS10CertificationRequest createCSR(KeyPair keyPair) throws FailedToCreateCSRException {
        try {
            X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                    .addRDN(BCStyle.CN, aiidaId.toString())
                    .addRDN(BCStyle.O, "EDDIE")
                    .addRDN(BCStyle.OU, "AIIDA")
                    .build();

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BC_PROVIDER) // BouncyCastle
                    .build(keyPair.getPrivate());

            return new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic()).build(signer);
        } catch (OperatorCreationException e) {
            throw new FailedToCreateCSRException("Failed to create CSR", e);
        }
    }

    private String sendCSRForSigning(String csrPem) throws FailedToGetCertificateException {
        try {
            return webClient.post()
                            .uri("/request")
                            .contentType(MediaType.TEXT_PLAIN)
                            .bodyValue(csrPem)
                            .retrieve()
                            .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                                    .flatMap(error -> {
                                        System.out.println(error);
                                        return reactor.core.publisher.Mono.error(new RuntimeException(error));
                                    })
                            )
                            .bodyToMono(String.class)
                            .block();
        } catch (Exception e) {
            throw new FailedToGetCertificateException("Error while sending CSR to Federator: " + e.getMessage());
        }
    }

    private String convertCSRToPem(PKCS10CertificationRequest csr) throws IOException {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(csr);
        }
        return writer.toString();
    }

    private String convertCertificateToPem(X509Certificate certificate) throws IOException {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(certificate);
        }
        return writer.toString();
    }

    private String convertByteArrayToPem(byte[] message) throws IOException {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(new PemObject("SIGNATURE", message));
        }
        return writer.toString();
    }

    private X509Certificate parsePemToCertificate(String pemCert) throws IOException, CertificateException {
        try (PEMParser pemParser = new PEMParser(new StringReader(pemCert))) {
            X509CertificateHolder certificateHolder = (X509CertificateHolder) pemParser.readObject();
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                                                       .generateCertificate(new ByteArrayInputStream(certificateHolder.getEncoded()));
        }
    }
}
