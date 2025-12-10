package energy.eddie.regionconnector.aiida.config;

import jakarta.annotation.Nullable;

public record PlainAiidaConfiguration(
        String customerId,
        int bCryptStrength,
        String handshakeUrl,
        String mqttServerUri,
        @Nullable String mqttPassword
) implements AiidaConfiguration {
    private static final String USERNAME = "eddie";

    @Override
    public String mqttUsername() {
        return USERNAME;
    }
}
