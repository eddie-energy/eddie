package energy.eddie.regionconnector.us.green.button.security;

import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.crypto.codec.Hex;
import tools.jackson.core.JacksonException;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class WebhookVerifier {
    public static final String WEBHOOK_SIGNATURE_HEADER = "X-UtilityAPI-Webhook-Signature";
    public static final String WEBHOOK_SALT_HEADER = "X-UtilityAPI-Webhook-Salt";
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookVerifier.class);
    @Nullable
    private final String salt;
    @Nullable
    private final String secret;
    private final InputStream inputStream;
    private final String signature;

    public WebhookVerifier(@Nullable String salt, @Nullable String secret, InputStream inputStream, String signature) {
        this.salt = salt;
        this.secret = secret;
        this.inputStream = inputStream;
        this.signature = signature;
    }

    public static AuthorizationDecision verifySignature(HttpServletRequest request, GreenButtonConfiguration config) {
        LOGGER.info("Verifying webhook signature");
        var signature = request.getHeader(WEBHOOK_SIGNATURE_HEADER);
        var salt = request.getHeader(WEBHOOK_SALT_HEADER);
        try {
            var verifier = new WebhookVerifier(salt, config.webhookSecret(), request.getInputStream(), signature);
            return new AuthorizationDecision(verifier.verifySignature());
        } catch (IOException e) {
            return new AuthorizationDecision(false);
        }
    }

    public boolean verifySignature() {
        if (signature == null || salt == null) {
            return false;
        }
        try {
            var body = prettyPrintJson(getRequestBody());
            var concatenation = "%s.%s.%s".formatted(secret, salt, body);
            var expectedSignature = hash(concatenation);
            return signature.equals(expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    private String getRequestBody() throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String hash(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encode(hashBytes));
    }

    private String prettyPrintJson(String uglyJsonString) throws JacksonException {
        DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
        var printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        var objectMapper = JsonMapper.builder()
                                     .defaultPrettyPrinter(printer)
                                     .build();
        var jsonObject = objectMapper.readValue(uglyJsonString, Object.class);
        return objectMapper.writerWithDefaultPrettyPrinter()
                           .writeValueAsString(jsonObject)
                           // Required since object mapper does not produce canonical json: https://github.com/FasterXML/jackson-core/issues/1037
                           .replace(" :", ":");
    }
}
