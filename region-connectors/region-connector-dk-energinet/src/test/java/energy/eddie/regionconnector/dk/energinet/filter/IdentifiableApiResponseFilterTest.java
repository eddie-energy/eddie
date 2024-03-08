package energy.eddie.regionconnector.dk.energinet.filter;

import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.PeriodtimeInterval;
import energy.eddie.regionconnector.dk.energinet.exceptions.ApiResponseException;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class IdentifiableApiResponseFilterTest {

    @Test
    void filter_returnsError_whenResponseContainsError() {
        // Arrange
        var permissionRequest = mock(DkEnerginetCustomerPermissionRequest.class);
        var response = mock(MyEnergyDataMarketDocumentResponse.class);
        when(response.getSuccess()).thenReturn(false);
        when(response.getErrorCode()).thenReturn(30000);
        when(response.getErrorText()).thenReturn("FromDateIsGreaterThanToday");

        LocalDate from = LocalDate.now(DK_ZONE_ID).minusDays(1);
        LocalDate to = from.plusDays(1);
        var filter = new IdentifiableApiResponseFilter(permissionRequest, from, to);

        // Act
        StepVerifier.create(filter.filter(List.of(response)))
                .expectError(ApiResponseException.class)
                .verify();
    }

    @Test
    void filter_returnEmpty_whenTimeIntervalNull() {
        // Arrange
        var permissionRequest = mock(DkEnerginetCustomerPermissionRequest.class);
        var response = mock(MyEnergyDataMarketDocumentResponse.class);
        when(response.getSuccess()).thenReturn(true);
        var document = mock(MyEnergyDataMarketDocument.class);
        when(document.getPeriodTimeInterval()).thenReturn(null);
        when(response.getMyEnergyDataMarketDocument()).thenReturn(document);

        LocalDate from = LocalDate.now(DK_ZONE_ID).minusDays(1);
        LocalDate to = from.plusDays(1);
        var filter = new IdentifiableApiResponseFilter(permissionRequest, from, to);

        // Act
        StepVerifier.create(filter.filter(List.of(response)))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void filter_updatesLastPolled_whenDateAfterLastPolled() {
        // Arrange
        var start = LocalDate.now().atStartOfDay(ZoneOffset.UTC);
        var end = start.plusDays(1);
        var permissionRequest = mock(DkEnerginetCustomerPermissionRequest.class);
        when(permissionRequest.lastPolled()).thenReturn(start.minusDays(1).withZoneSameInstant(DK_ZONE_ID));
        var response = mock(MyEnergyDataMarketDocumentResponse.class);
        when(response.getSuccess()).thenReturn(true);
        var document = mock(MyEnergyDataMarketDocument.class);
        when(document.getPeriodTimeInterval()).thenReturn(new PeriodtimeInterval().start(start.toString()).end(end.toString()));
        when(response.getMyEnergyDataMarketDocument()).thenReturn(document);
        when(response.getMyEnergyDataMarketDocument()).thenReturn(document);

        var filter = new IdentifiableApiResponseFilter(permissionRequest, start.toLocalDate(), end.toLocalDate());

        // Act
        StepVerifier.create(filter.filter(List.of(response)))
                .assertNext(apiResponse -> assertEquals(response, apiResponse.apiResponse()))
                .verifyComplete();
        verify(permissionRequest).updateLastPolled(end.withZoneSameInstant(DK_ZONE_ID));
    }

    @Test
    void filter_doesNotUpdateLastPolled_whenDateBeforeLastPolled() {
        // Arrange
        var start = LocalDate.now().atStartOfDay(ZoneOffset.UTC);
        var end = start.plusDays(1);
        var permissionRequest = mock(DkEnerginetCustomerPermissionRequest.class);
        when(permissionRequest.lastPolled()).thenReturn(start.plusDays(1).withZoneSameInstant(DK_ZONE_ID));
        var response = mock(MyEnergyDataMarketDocumentResponse.class);
        when(response.getSuccess()).thenReturn(true);
        var document = mock(MyEnergyDataMarketDocument.class);
        when(document.getPeriodTimeInterval()).thenReturn(new PeriodtimeInterval().start(start.toString()).end(end.toString()));
        when(response.getMyEnergyDataMarketDocument()).thenReturn(document);
        when(response.getMyEnergyDataMarketDocument()).thenReturn(document);

        var filter = new IdentifiableApiResponseFilter(permissionRequest, start.toLocalDate(), end.toLocalDate());

        // Act
        StepVerifier.create(filter.filter(List.of(response)))
                .assertNext(apiResponse -> assertEquals(response, apiResponse.apiResponse()))
                .verifyComplete();
        verify(permissionRequest, never()).updateLastPolled(any());
    }
}
