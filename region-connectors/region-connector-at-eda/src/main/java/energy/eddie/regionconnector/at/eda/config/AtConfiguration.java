package energy.eddie.regionconnector.at.eda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

/**
 * @param eligiblePartyId ID that will be used as the sender for all messages sent to EDA.
 *                        This ID must be registered with EDA at <a href="https://www.ebutilities.at/registrierung">ebUtilities</a>.
 */
@ConfigurationProperties("region-connector.at.eda")
public record AtConfiguration(@Name("eligibleparty.id") String eligiblePartyId) {}
