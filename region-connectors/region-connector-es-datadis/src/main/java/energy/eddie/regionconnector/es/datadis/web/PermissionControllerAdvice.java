// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.regionconnector.es.datadis.exceptions.EsValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class PermissionControllerAdvice {
    @ExceptionHandler(EsValidationException.class)
    public ResponseEntity<Map<String,List<EddieApiError>>> handleEsValidationException(EsValidationException e) {
        var error = e.error();
        return ResponseEntity.badRequest()
                             .body(Map.of("errors", List.of(new EddieApiError(error.message()))));
    }
}
