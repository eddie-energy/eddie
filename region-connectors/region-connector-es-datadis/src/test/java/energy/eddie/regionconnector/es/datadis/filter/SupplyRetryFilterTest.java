package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplyRetryFilterTest {

    @Test
    void filter_ifUnauthorized_returnsFalse() {
        var filter = new SupplyRetryFilter(new DatadisApiException("", HttpResponseStatus.UNAUTHORIZED, ""));
        assertFalse(filter.filter());
    }

    @Test
    void filter_ifBadRequest_returnsFalse() {
        var filter = new SupplyRetryFilter(new DatadisApiException("", HttpResponseStatus.UNAUTHORIZED, ""));
        assertFalse(filter.filter());
    }

    @Test
    void filter_ifInternalServerError_returnsTrue() {
        var filter = new SupplyRetryFilter(new DatadisApiException("", HttpResponseStatus.INTERNAL_SERVER_ERROR, ""));
        assertTrue(filter.filter());
    }

    @Test
    void filter_ifNestedRuntimeException_returnsTrue() {
        var filter = new SupplyRetryFilter(new RuntimeException(new RuntimeException()));
        assertTrue(filter.filter());
    }
}