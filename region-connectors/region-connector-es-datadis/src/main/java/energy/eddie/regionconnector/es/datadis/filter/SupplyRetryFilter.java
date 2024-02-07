package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import org.springframework.http.HttpStatus;

public record SupplyRetryFilter(Throwable throwable) {

    public boolean filter() {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof DatadisApiException apiException) {
            return !(apiException.statusCode() == HttpStatus.UNAUTHORIZED.value() ||
                    apiException.statusCode() == HttpStatus.BAD_REQUEST.value());
        }
        return true;
    }
}