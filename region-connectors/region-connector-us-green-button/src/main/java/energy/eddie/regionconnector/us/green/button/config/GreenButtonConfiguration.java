package energy.eddie.regionconnector.us.green.button.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.TreeMap;

@ConfigurationProperties(value = "region-connector.us.green.button")
public class GreenButtonConfiguration {
    private final String apiToken;
    private final String basePath;
    private final Map<String, String> clientIds = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, String> clientSecrets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final String redirectUri;
    private final int activationBatchSize;

    public GreenButtonConfiguration(
            String apiToken,
            String basePath,
            Map<String, String> clientIds,
            Map<String, String> clientSecrets,
            String redirectUri,
            int activationBatchSize
    ) {
        this.apiToken = apiToken;
        this.basePath = basePath;
        this.clientIds.putAll(clientIds);
        this.clientSecrets.putAll(clientSecrets);
        this.redirectUri = redirectUri;
        this.activationBatchSize = activationBatchSize;
    }

    public String apiToken() {return apiToken;}

    public String basePath() {return basePath;}

    public Map<String, String> clientIds() {return clientIds;}

    public Map<String, String> clientSecrets() {return clientSecrets;}

    public String redirectUri() {return redirectUri;}

    public int activationBatchSize() {return activationBatchSize;}
}
