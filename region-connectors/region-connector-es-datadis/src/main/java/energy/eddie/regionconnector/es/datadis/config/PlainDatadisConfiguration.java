package energy.eddie.regionconnector.es.datadis.config;

public record PlainDatadisConfiguration(String username,
                                        String password,
                                        String basePath,
                                        int timeoutDuration) implements DatadisConfig {
}