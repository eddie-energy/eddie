package energy.eddie.regionconnector.at.eda.config;

import java.util.Optional;

public interface AtConfiguration {
    String PREFIX = "region-connector.at.eda.";

    String ELIGIBLE_PARTY_ID_KEY = PREFIX + "eligibleparty.id";
    String CONVERSATION_ID_PREFIX = PREFIX + "conversation.id.prefix";

    /**
     * ID that will be used as the sender for all messages sent to EDA. This ID must be registered with EDA at <a href="https://www.ebutilities.at/registrierung">ebUtilities</a>.
     */
    String eligiblePartyId();

    /**
     * Used to prefix the conversation id to enable routing between different ponton xp adapters.
     * Can be omitted if not needed.
     */
    Optional<String> conversationIdPrefix();
}
