package energy.eddie.regionconnector.cds.services.client.creation.responses;

import energy.eddie.regionconnector.cds.master.data.CdsServer;

public record CreatedCdsClientResponse(CdsServer cdsServer) implements ApiClientCreationResponse {
}
