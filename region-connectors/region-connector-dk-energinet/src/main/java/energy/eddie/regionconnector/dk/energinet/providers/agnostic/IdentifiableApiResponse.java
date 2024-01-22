package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;

import java.util.List;

public record IdentifiableApiResponse(String permissionId,
                                      String connectionId,
                                      String dataNeedId,
                                      List<MyEnergyDataMarketDocumentResponse> apiResponse) {
}
