package energy.eddie.regionconnector.at.eda.ponton;

public record PlainPontonXPAdapterConfiguration(String adapterId, String adapterVersion, String hostname, int port,
                                                String workFolder) implements PontonXPAdapterConfiguration {
}
