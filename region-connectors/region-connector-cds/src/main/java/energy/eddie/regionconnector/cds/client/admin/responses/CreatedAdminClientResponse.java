package energy.eddie.regionconnector.cds.client.admin.responses;

import energy.eddie.regionconnector.cds.client.admin.AdminClient;

public record CreatedAdminClientResponse(AdminClient apiClient) implements ApiClientCreationResponse {
}
