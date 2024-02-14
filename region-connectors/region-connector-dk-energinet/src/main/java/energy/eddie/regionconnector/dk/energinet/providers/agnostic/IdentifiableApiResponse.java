package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;

public record IdentifiableApiResponse(String permissionId,
                                      String connectionId,
                                      String dataNeedId,
                                      MyEnergyDataMarketDocumentResponse apiResponse) {
}