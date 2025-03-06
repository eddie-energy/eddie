package energy.eddie.regionconnector.cds.services.client.creation.responses;

public sealed interface ApiClientCreationResponse permits CreatedCdsClientResponse, NotACdsServerResponse, UnableToRegisterClientResponse, UnsupportedFeatureResponse {
}
