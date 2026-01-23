// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.InvalidPointAndMeasurementTypeCombinationException;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatadisApiRetryFilterTest {

    @Test
    void filter_ifUnauthorized_returnsFalse() {
        var filter = new DatadisApiRetryFilter(
                DatadisApiRetryFilterTest.class,
                List.of()
        );
        assertFalse(filter.filter(new DatadisApiException("", HttpResponseStatus.UNAUTHORIZED, "")));
    }

    @Test
    void filter_ifInternalServerError_returnsTrue() {
        var filter = new DatadisApiRetryFilter(

                DatadisApiRetryFilterTest.class,
                List.of()
        );
        assertTrue(filter.filter(new DatadisApiException("", HttpResponseStatus.INTERNAL_SERVER_ERROR, "")));
    }

    @Test
    void filter_ifNonRetryableException_returnsFalse() {
        var filter = new DatadisApiRetryFilter(

                DatadisApiRetryFilterTest.class,
                List.of(InvalidPointAndMeasurementTypeCombinationException.class)
        );
        var exception = new InvalidPointAndMeasurementTypeCombinationException(4, MeasurementType.QUARTER_HOURLY);
        assertFalse(filter.filter(exception));
    }

    @Test
    void filter_ifNestedRuntimeException_returnsTrue() {
        var filter = new DatadisApiRetryFilter(
                DatadisApiRetryFilterTest.class,
                List.of()
        );
        assertTrue(filter.filter(new RuntimeException(new RuntimeException())));
    }
}
