package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "marketplace")
public class MarketplaceConfiguration {
    private final String marketplaceFederatorUrl;
    private final String keystorePath;
    private final char[] keystorePassword;

    public MarketplaceConfiguration(String federatorUrl, String keystorePath, char[] keystorePassword) {
        this.marketplaceFederatorUrl = federatorUrl;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
    }

    public String getMarketplaceFederatorUrl() {
        return marketplaceFederatorUrl;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public char[] getKeystorePassword() {
        return keystorePassword;
    }
}
