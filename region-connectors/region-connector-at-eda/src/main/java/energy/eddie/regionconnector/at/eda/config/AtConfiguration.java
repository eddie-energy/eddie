package energy.eddie.regionconnector.at.eda.config;

public interface AtConfiguration {
    String PREFIX = "region-connector.at.eda.";

    String ELIGIBLE_PARTY_ID_KEY = PREFIX + "eligibleparty.id";


    /**
     * ID that will be used as the sender for all messages sent to EDA. This ID must be registered with EDA at <a
     * href="https://www.ebutilities.at/registrierung">ebUtilities</a>.
     */
    String eligiblePartyId();
}
