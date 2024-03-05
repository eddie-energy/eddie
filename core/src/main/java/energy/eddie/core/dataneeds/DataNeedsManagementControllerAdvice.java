package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.core.dataneeds.exceptions.DataNeedAlreadyExistsException;
import energy.eddie.core.dataneeds.exceptions.DataNeedIdsNotMatchingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;

@ControllerAdvice
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
public class DataNeedsManagementControllerAdvice {
    @ExceptionHandler(DataNeedAlreadyExistsException.class)
    // DataNeedAlreadyExistsException#getMessage() always returns a not null String
    @SuppressWarnings("NullAway")
    public ResponseEntity<Map<String, List<EddieApiError>>> handleDataNeedAlreadyExistsException
            (DataNeedAlreadyExistsException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
    }

    @ExceptionHandler(DataNeedIdsNotMatchingException.class)
    // DataNeedIdsNotMatchingException#getMessage() always returns a not null String
    @SuppressWarnings("NullAway")
    public ResponseEntity<Map<String, List<EddieApiError>>> handleDataNeedIdsNotMatchingException
            (DataNeedIdsNotMatchingException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.badRequest().body(errors);
    }
}
