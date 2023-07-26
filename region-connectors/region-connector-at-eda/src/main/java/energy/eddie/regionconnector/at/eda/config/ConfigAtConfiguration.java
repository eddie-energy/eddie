package energy.eddie.regionconnector.at.eda.config;

import org.eclipse.microprofile.config.Config;

import static java.util.Objects.requireNonNull;

public class ConfigAtConfiguration implements AtConfiguration {
    private final Config config;

    public ConfigAtConfiguration(Config config) {
        requireNonNull(config);
        this.config = config;

        // check for the presence of the required configuration keys
        eligiblePartyId();
    }

    @Override
    public String eligiblePartyId() {
        return config.getValue(ELIGIBLE_PARTY_ID_KEY, String.class);
    }
}
