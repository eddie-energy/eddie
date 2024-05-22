package energy.eddie.regionconnector.dk.energinet.filter;

import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDto;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDtoResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.exceptions.ApiResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public record MeteringDetailsApiResponseFilter() {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableApiResponseFilter.class);

    public Mono<MeteringPointDetailsCustomerDto> filter(
            String meteringPointId,
            MeteringPointDetailsCustomerDtoResponseListApiResponse response
    ) {
        var result = response.getResult();
        if (result == null || result.isEmpty()) {
            LOGGER.warn("No metering point details found for meteringPointId {}", meteringPointId);
            return Mono.empty();
        }

        // since we always request a single metering point, we always expect a single result
        var marketDocumentResponse = result.getFirst();
        if (Boolean.FALSE.equals(marketDocumentResponse.getSuccess())) {
            return Mono.error(new ApiResponseException(marketDocumentResponse.getErrorCode(),
                                                       marketDocumentResponse.getErrorText()));
        }

        MeteringPointDetailsCustomerDto details = marketDocumentResponse.getResult();
        if (details == null) {
            LOGGER.warn("No metering point details found for meteringPointId {}", meteringPointId);
            return Mono.empty();
        }

        return Mono.just(details);
    }
}
