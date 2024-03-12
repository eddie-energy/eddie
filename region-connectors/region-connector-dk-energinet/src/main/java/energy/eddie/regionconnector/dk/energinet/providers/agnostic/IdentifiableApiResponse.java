package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;

public record IdentifiableApiResponse(
        DkEnerginetCustomerPermissionRequest permissionRequest,
        MyEnergyDataMarketDocumentResponse apiResponse
) {
}
