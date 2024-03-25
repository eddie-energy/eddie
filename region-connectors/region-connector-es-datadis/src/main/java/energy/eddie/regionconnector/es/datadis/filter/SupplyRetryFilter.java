package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public record SupplyRetryFilter(Throwable throwable) {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupplyRetryFilter.class);

    public boolean filter() {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof DatadisApiException apiException && (apiException.statusCode() == HttpStatus.UNAUTHORIZED.value() ||
                apiException.statusCode() == HttpStatus.BAD_REQUEST.value())) {
            return false;
        }
        LOGGER.info("Retrying supply request due to exception", cause);
        return true;
    }
}
