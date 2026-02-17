// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import energy.eddie.regionconnector.aiida.exceptions.DataNeedInvalidException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;

@RestControllerAdvice
public class ControllerAdvice {
    @ExceptionHandler({
            CredentialsAlreadyExistException.class,
            DataNeedInvalidException.class
    })
    public ResponseEntity<Map<String, List<EddieApiError>>> handleBadRequestException(Exception exception) {
        @SuppressWarnings("NullAway")  // CredentialsAlreadyExistException always has a message
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.badRequest().body(errors);
    }
}
