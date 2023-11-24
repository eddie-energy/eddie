package energy.eddie.regionconnector.dk.energinet.config;

public record PlainEnerginetConfiguration(
        String customerBasePath,
        String thirdPartyBasePath
) implements EnerginetConfiguration {
}
