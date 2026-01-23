// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.filter;

import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDto;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDtoResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDtoResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.exceptions.ApiResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeteringDetailsApiResponseFilterTest {

    private static Stream<MeteringPointDetailsCustomerDtoResponseListApiResponse> emptyResponses() {
        return Stream.of(
                new MeteringPointDetailsCustomerDtoResponseListApiResponse(),
                new MeteringPointDetailsCustomerDtoResponseListApiResponse()
                        .addResultItem(new MeteringPointDetailsCustomerDtoResponse())
        );
    }

    @ParameterizedTest
    @MethodSource("emptyResponses")
    void filter_returnsEmptyIfResponseEmpty(MeteringPointDetailsCustomerDtoResponseListApiResponse emptyResponses) {
        // Arrange
        MeteringDetailsApiResponseFilter filter = new MeteringDetailsApiResponseFilter();
        String meteringPointId = "meteringPointId";
        var result = filter.filter(meteringPointId, emptyResponses);

        // Assert
        result.as(StepVerifier::create)
              .expectNextCount(0)
              .verifyComplete();
    }

    @Test
    void filter_returnsErrorIfResponseDoesNotIndicateSuccess() {
        // Arrange
        MeteringDetailsApiResponseFilter filter = new MeteringDetailsApiResponseFilter();
        String meteringPointId = "meteringPointId";
        MeteringPointDetailsCustomerDtoResponse errorResponse = mock(MeteringPointDetailsCustomerDtoResponse.class);
        when(errorResponse.getSuccess()).thenReturn(false);
        when(errorResponse.getErrorCode()).thenReturn(2000);
        when(errorResponse.getErrorText()).thenReturn("errorText");
        MeteringPointDetailsCustomerDtoResponseListApiResponse response = new MeteringPointDetailsCustomerDtoResponseListApiResponse()
                .addResultItem(errorResponse);
        var result = filter.filter(meteringPointId, response);

        // Assert
        result.as(StepVerifier::create)
              .expectErrorMatches(e -> e instanceof ApiResponseException &&
                                       ((ApiResponseException) e).errorCode() == 2000 &&
                                       ((ApiResponseException) e).errorText().equals("errorText"))
              .verify();
    }

    @Test
    void filter_returnsDetailsOnSuccess() {
        // Arrange
        MeteringDetailsApiResponseFilter filter = new MeteringDetailsApiResponseFilter();
        String meteringPointId = "meteringPointId";
        MeteringPointDetailsCustomerDto details = new MeteringPointDetailsCustomerDto();
        MeteringPointDetailsCustomerDtoResponseListApiResponse response = new MeteringPointDetailsCustomerDtoResponseListApiResponse()
                .addResultItem(
                        new MeteringPointDetailsCustomerDtoResponse()
                                .success(true)
                                .result(details));
        var result = filter.filter(meteringPointId, response);

        // Assert
        result.as(StepVerifier::create)
              .assertNext(next -> assertEquals(details, next))
              .expectComplete()
              .verify();
    }
}
