// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.List;

public record DatadisApiRetryFilter(Class<?> requestType, List<Class<?>> nonRetryableExceptions) {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatadisApiRetryFilter.class);

    public boolean filter(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof DatadisApiException apiException && (apiException.statusCode() == HttpStatus.UNAUTHORIZED.value() ||
                apiException.statusCode() == HttpStatus.BAD_REQUEST.value())) {
            return false;
        }
        for (Class<?> nonRetryableException : nonRetryableExceptions) {
            if (nonRetryableException.isInstance(cause)) {
                return false;
            }
        }

        LOGGER.info("Retrying {} request due to exception", requestType.getName(), cause);
        return true;
    }
}
