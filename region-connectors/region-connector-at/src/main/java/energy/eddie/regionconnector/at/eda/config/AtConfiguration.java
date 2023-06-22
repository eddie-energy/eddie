package energy.eddie.regionconnector.at.eda.config;

import java.time.ZoneId;

public interface AtConfiguration {

    /**
     * ID that will be used as the sender for all messages sent to EDA. This ID must be registered with EDA at <a href="https://www.ebutilities.at/registrierung">ebUtilities</a>.
     */
    String eligiblePartyId();

    /**
     * The time zone that will be used for all datetime values sent to EDA and received from EDA.
     */
    ZoneId timeZone();
}
