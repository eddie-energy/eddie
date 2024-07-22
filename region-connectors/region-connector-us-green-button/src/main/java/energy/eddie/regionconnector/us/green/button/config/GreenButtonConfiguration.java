package energy.eddie.regionconnector.us.green.button.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(value = "region-connector.us.green.button")
public class GreenButtonConfiguration {
    private final String apiToken;
    private final String basePath;
    private final Map<String, String> clientIds;
    private final Map<String, String> clientSecrets;
    private final String redirectUrl;

    @SuppressWarnings("java:S107")
    // Config class is only instantiated by spring
    public GreenButtonConfiguration(
            String apiToken,
            String basePath,
            Map<String, String> clientIds,
            Map<String, String> clientSecrets,
            String redirectUrl
    ) {
        this.apiToken = apiToken;
        this.basePath = basePath;
        this.clientIds = clientIds;
        this.clientSecrets = clientSecrets;
        this.redirectUrl = redirectUrl;
    }

    public String apiToken() {return apiToken;}

    public String basePath() {return basePath;}

    public Map<String, String> clientIds() {return clientIds;}

    public Map<String, String> clientSecrets() {return clientSecrets;}

    public String redirectUrl() {return redirectUrl;}
}
