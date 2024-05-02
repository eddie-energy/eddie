package energy.eddie.regionconnector.aiida.config;

public record PlainAiidaConfiguration(
        String customerId,
        int bCryptStrength,
        String handshakeUrl
) implements AiidaConfiguration {}
