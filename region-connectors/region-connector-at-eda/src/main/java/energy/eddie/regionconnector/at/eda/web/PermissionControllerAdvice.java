package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.regionconnector.at.eda.services.EdaValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class PermissionControllerAdvice {

    @ExceptionHandler(EdaValidationException.class)
    public ResponseEntity<List<AttributeError>> handleEdaValidationException(
            EdaValidationException exception
    ) {
        return ResponseEntity.badRequest().body(exception.errors());
    }
}
