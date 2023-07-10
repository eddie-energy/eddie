package energy.eddie.regionconnector.at.eda.config;

public interface AtConfiguration {

    /**
     * ID that will be used as the sender for all messages sent to EDA. This ID must be registered with EDA at <a href="https://www.ebutilities.at/registrierung">ebUtilities</a>.
     */
    String eligiblePartyId();
}
