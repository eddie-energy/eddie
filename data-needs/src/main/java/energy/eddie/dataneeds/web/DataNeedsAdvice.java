package energy.eddie.dataneeds.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;

@RegionConnectorExtension
@RestControllerAdvice
public class DataNeedsAdvice {
    @ExceptionHandler(DataNeedNotFoundException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleDataNeedNotFoundException
            (DataNeedNotFoundException exception) {
        @SuppressWarnings("NullAway")  // DataNeedNotFoundException always has a message
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));

        return (exception.isBadRequest() ? ResponseEntity.badRequest() : ResponseEntity.status(HttpStatus.NOT_FOUND))
                .body(errors);
    }
}
