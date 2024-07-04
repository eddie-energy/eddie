package energy.eddie.regionconnector.fi.fingrid.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("region-connector.fi.fingrid")
public class FingridConfiguration {
    private final String organisationUser;
    private final String organisationName;

    public FingridConfiguration(String organisationUser, String organisationName) {
        this.organisationUser = organisationUser;
        this.organisationName = organisationName;
    }

    public String organisationUser() {
        return organisationUser;
    }

    public String organisationName() {
        return organisationName;
    }
}
