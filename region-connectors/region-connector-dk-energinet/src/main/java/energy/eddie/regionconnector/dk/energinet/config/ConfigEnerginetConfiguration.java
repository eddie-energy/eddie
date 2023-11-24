package energy.eddie.regionconnector.dk.energinet.config;

import org.eclipse.microprofile.config.Config;

import static java.util.Objects.requireNonNull;

public class ConfigEnerginetConfiguration implements EnerginetConfiguration {
    private final Config config;

    public ConfigEnerginetConfiguration(Config config) {
        requireNonNull(config);
        this.config = config;
    }

    @Override
    public String customerBasePath() {
        return config.getValue(ENERGINET_CUSTOMER_BASE_PATH_KEY, String.class);
    }

    @Override
    public String thirdPartyBasePath() {
        return config.getValue(ENERGINET_THIRD_PARTY_BASE_PATH_KEY, String.class);
    }
}
