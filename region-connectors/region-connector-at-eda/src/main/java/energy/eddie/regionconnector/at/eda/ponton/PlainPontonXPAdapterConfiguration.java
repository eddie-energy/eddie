package energy.eddie.regionconnector.at.eda.ponton;

public record PlainPontonXPAdapterConfiguration(
        String adapterId,
        String adapterVersion,
        String hostname,
        int port,
        String apiEndpoint,
        String workFolder,
        String username,
        String password
) implements PontonXPAdapterConfiguration {
}
