package energy.eddie.regionconnector.dk.energinet.filter;

import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.exceptions.ApiResponseException;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record IdentifiableApiResponseFilter(DkEnerginetCustomerPermissionRequest permissionRequest,
                                            LocalDate dateFrom,
                                            LocalDate dateTo) {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableApiResponseFilter.class);

    public Mono<IdentifiableApiResponse> filter(List<MyEnergyDataMarketDocumentResponse> response) {
        var marketDocumentResponse = response.getFirst();
        if (Boolean.FALSE.equals(marketDocumentResponse.getSuccess())) {
            return Mono.error(new ApiResponseException(marketDocumentResponse.getErrorCode(), marketDocumentResponse.getErrorText()));
        }
        var timeInterval = Optional.ofNullable(marketDocumentResponse.getMyEnergyDataMarketDocument()).map(MyEnergyDataMarketDocument::getPeriodTimeInterval);
        var permissionId = permissionRequest.permissionId();
        if (timeInterval.isEmpty()) {
            LOGGER.warn("No metering data present in request for permissionId {} from {} to {}", permissionId, dateFrom, dateTo);
            return Mono.empty();
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Fetched metering data for permissionId {} from {} to {}, received data from {} to {}", permissionId, dateFrom, dateTo, timeInterval.get().getStart(), timeInterval.get().getEnd());
        }

        return Mono.just(
                new IdentifiableApiResponse(
                        permissionRequest,
                        marketDocumentResponse));
    }
}
