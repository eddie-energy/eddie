// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.regionconnector.nl.mijn.aansluiting.exceptions.NlValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;

@RestControllerAdvice
public class ControllerAdvice {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerAdvice.class);

    @ExceptionHandler(URISyntaxException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleURISyntaxException(
            URISyntaxException uriSyntaxException
    ) {
        LOGGER.warn("Failed to create an URI", uriSyntaxException);

        var errorMsg = "OAuth processing failed. Please contact service provider.";
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(errorMsg)));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(NlValidationException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleNlValidationException(
            NlValidationException exception
    ) {
        LOGGER.info("Failed to create permission request", exception);
        var error = exception.error();
        return ResponseEntity.badRequest()
                             .body(Map.of(error.name(), List.of(new EddieApiError(error.message()))));
    }
}
