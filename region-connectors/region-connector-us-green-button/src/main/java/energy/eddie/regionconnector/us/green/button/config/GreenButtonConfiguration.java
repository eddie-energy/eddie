package energy.eddie.regionconnector.us.green.button.config;

import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingApiTokenException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.TreeMap;

import static energy.eddie.regionconnector.us.green.button.security.WebhookSecurityConfig.US_GREEN_BUTTON_ENABLED;

@ConfigurationProperties(value = "region-connector.us.green.button")
@ConditionalOnProperty(value = US_GREEN_BUTTON_ENABLED, havingValue = "true")
public class GreenButtonConfiguration {
    private final String basePath;
    private final Map<String, String> clientIds = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, String> clientSecrets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, String> tokens = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final String redirectUri;
    private final String webhookSecret;

    public GreenButtonConfiguration(
            String basePath,
            Map<String, String> clientIds,
            Map<String, String> clientSecrets,
            Map<String, String> tokens,
            String redirectUri,
            String webhookSecret
    ) {
        this.basePath = basePath;
        this.clientIds.putAll(clientIds);
        this.clientSecrets.putAll(clientSecrets);
        this.tokens.putAll(tokens);
        this.redirectUri = redirectUri;
        this.webhookSecret = webhookSecret;
    }


    public String basePath() {return basePath;}

    public Map<String, String> clientIds() {return clientIds;}

    public Map<String, String> clientSecrets() {return clientSecrets;}

    public Map<String, String> tokens() {return tokens;}

    public String redirectUri() {return redirectUri;}

    public String webhookSecret() {return webhookSecret;}

    public String getClientIdOrThrow(String company) throws MissingClientIdException {
        return getOrThrow(clientIds, company, new MissingClientIdException(company));
    }

    public String getClientSecretOrThrow(String company) throws MissingClientSecretException {
        return getOrThrow(clientSecrets, company, new MissingClientSecretException(company));
    }

    public void throwOnMissingToken(String company) throws MissingApiTokenException {
        getOrThrow(tokens, company, new MissingApiTokenException(company));
    }


    private static <T extends Exception> String getOrThrow(
            Map<String, String> credentials,
            String key,
            T exception
    ) throws T {
        if (credentials.containsKey(key)) {
            return credentials.get(key);
        }
        throw exception;
    }
}
