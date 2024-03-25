package energy.eddie.regionconnector.dk.energinet.filter;

import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.PeriodtimeInterval;
import energy.eddie.regionconnector.dk.energinet.exceptions.ApiResponseException;
import energy.eddie.regionconnector.dk.energinet.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class IdentifiableApiResponseFilterTest {

    @Test
    void filter_returnsError_whenResponseContainsError() {
        // Given
        var permissionRequest = new SimplePermissionRequest();
        new MyEnergyDataMarketDocumentResponse(30000);

        var response = new MyEnergyDataMarketDocumentResponse(30000);
        response.success(false);
        response.errorText("FromDateIsGreaterThanToday");

        LocalDate from = LocalDate.now(DK_ZONE_ID).minusDays(1);
        LocalDate to = from.plusDays(1);
        var filter = new IdentifiableApiResponseFilter(permissionRequest, from, to);

        // When & Then
        StepVerifier.create(filter.filter(List.of(response)))
                    .expectError(ApiResponseException.class)
                    .verify();
    }

    @Test
    void filter_returnEmpty_whenTimeIntervalNull() {
        // Given
        var permissionRequest = new SimplePermissionRequest();
        var response = new MyEnergyDataMarketDocumentResponse(0);
        response.success(true);
        var document = new MyEnergyDataMarketDocument();
        response.setMyEnergyDataMarketDocument(document);

        LocalDate from = LocalDate.now(DK_ZONE_ID).minusDays(1);
        LocalDate to = from.plusDays(1);
        var filter = new IdentifiableApiResponseFilter(permissionRequest, from, to);

        // When & Then
        StepVerifier.create(filter.filter(List.of(response)))
                    .expectNextCount(0)
                    .verifyComplete();
    }

    @Test
    void filter_doesNotUpdateLastPolled_whenDateBeforeLastPolled() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID);
        var end = start.plusDays(1);
        var permissionRequest = new SimplePermissionRequest(start, end, start.plusDays(1));
        DkEnerginetCustomerPermissionRequest spy = spy(permissionRequest);
        var response = new MyEnergyDataMarketDocumentResponse(0);
        response.success(true);
        var document = new MyEnergyDataMarketDocument();
        document.setPeriodTimeInterval(new PeriodtimeInterval().start(start.toString()).end(end.toString()));
        response.setMyEnergyDataMarketDocument(document);

        var filter = new IdentifiableApiResponseFilter(spy, start, end);

        // When & Then
        StepVerifier.create(filter.filter(List.of(response)))
                    .assertNext(apiResponse -> assertEquals(response, apiResponse.apiResponse()))
                    .verifyComplete();
        verify(spy, never()).updateLatestMeterReadingEndDate(any());
    }
}
