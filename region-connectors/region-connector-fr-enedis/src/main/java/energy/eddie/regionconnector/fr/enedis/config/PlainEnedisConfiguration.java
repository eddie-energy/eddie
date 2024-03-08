package energy.eddie.regionconnector.fr.enedis.config;

public record PlainEnedisConfiguration(
        String clientId,
        String clientSecret,
        String basePath,
        int timeoutDuration
) implements EnedisConfiguration {
}
