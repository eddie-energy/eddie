package energy.eddie.regionconnector.cds.client.responses;

import energy.eddie.regionconnector.cds.client.CdsApiClient;

public record CreatedApiClientResponse(CdsApiClient apiClient) implements ApiClientCreationResponse {
}
