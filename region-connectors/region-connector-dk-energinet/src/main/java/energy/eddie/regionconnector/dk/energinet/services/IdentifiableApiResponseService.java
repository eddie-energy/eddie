package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;

@Service
public class IdentifiableApiResponseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableApiResponseService.class);

    public IdentifiableApiResponseService(Flux<IdentifiableApiResponse> identifiableApiResponseFlux) {
        identifiableApiResponseFlux.subscribe(this::updateLastPolledAndCheckForFulfillment);
    }

    private void updateLastPolledAndCheckForFulfillment(IdentifiableApiResponse identifiableApiResponse) {
        var permissionRequest = identifiableApiResponse.permissionRequest();
        var permissionId = permissionRequest.permissionId();

        var marketDocumentResponse = identifiableApiResponse.apiResponse();
        var meterReadingEndDate = extractEndDate(marketDocumentResponse);

        if (isLatestMeterReading(permissionRequest, meterReadingEndDate)) {
            LOGGER.info("Updating latest meter reading for permission request {} from {} to {}",
                        permissionId,
                        permissionRequest.lastPolled(),
                        meterReadingEndDate);
            permissionRequest.updateLastPolled(meterReadingEndDate);

            if (isFulfilled(permissionRequest, meterReadingEndDate)) {
                LOGGER.info("Fulfilling permission request {}", permissionId);
                try {
                    permissionRequest.fulfill();
                    LOGGER.info("Permission request {} fulfilled", permissionId);
                } catch (StateTransitionException e) {
                    LOGGER.error("Error while fulfilling permission request {}", permissionId, e);
                }
            }
        }
    }

    @SuppressWarnings("DataFlowIssue") // IdentifiableApiResponseFilter ensures that none of these getters return null
    private static LocalDate extractEndDate(MyEnergyDataMarketDocumentResponse marketDocumentResponse) {
        var timeInterval = marketDocumentResponse.getMyEnergyDataMarketDocument().getPeriodTimeInterval().getEnd();
        return parseZonedDateTime(timeInterval);
    }

    private static boolean isLatestMeterReading(
            DkEnerginetCustomerPermissionRequest permissionRequest,
            LocalDate meterReadingEndDate
    ) {
        return meterReadingEndDate.isAfter(permissionRequest.lastPolled());
    }

    /**
     * Checks if the permission request is fulfilled. The last metering data date is always at 00:00 the next day, so if
     * we get data for the 24.01.2024, the last metering data date will be 25.01.2024T00:00:00 when converted to the
     * correct timezone this check makes sure that the permission request is fulfilled only if the last metering data
     * date is after the permission end date (since we also want data for the end date of the permission request)
     *
     * @param permissionRequest   the permission request
     * @param meterReadingEndDate the end date of the meter reading
     * @return true if {@code meterReadingEndDate} is after {@code permissionRequest.end()}
     */
    private static boolean isFulfilled(
            DkEnerginetCustomerPermissionRequest permissionRequest,
            LocalDate meterReadingEndDate
    ) {
        return Optional.ofNullable(permissionRequest.end())
                       .map(ZonedDateTime::toLocalDate)
                       .map(meterReadingEndDate::isAfter)
                       .orElse(false);
    }

    private static LocalDate parseZonedDateTime(String zonedDateTime) {
        // This ensures that a received datetime: 2024-03-05T23:00:00Z is parsed to 2024-03-06T00:00:00+01:00
        return ZonedDateTime.parse(zonedDateTime, DateTimeFormatter.ISO_DATE_TIME)
                            .withZoneSameInstant(DK_ZONE_ID)
                            .toLocalDate();
    }
}
