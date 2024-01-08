package energy.eddie.regionconnector.at.eda.config;

import jakarta.annotation.Nullable;

import java.util.Optional;

public record PlainAtConfiguration(String eligiblePartyId,
                                   @Nullable String optionalConversationIdPrefix) implements AtConfiguration {

    @Override
    public Optional<String> conversationIdPrefix() {
        return Optional.ofNullable(optionalConversationIdPrefix());
    }
}
