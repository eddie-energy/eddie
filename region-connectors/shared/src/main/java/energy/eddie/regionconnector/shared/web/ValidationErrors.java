package energy.eddie.regionconnector.shared.web;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashMap;
import java.util.Map;


public class ValidationErrors implements ErrorMapper {

    private final MethodArgumentNotValidException exception;

    public ValidationErrors(MethodArgumentNotValidException exception) {
        this.exception = exception;
    }

    @Override
    public Map<String, String> asMap() {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                String fieldName = fieldError.getField();
                String errorMessage = fieldError.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            } else {
                String objectName = error.getObjectName();
                String errorMessage = error.getDefaultMessage();
                errors.put(objectName, errorMessage);
            }
        });
        return errors;
    }
}
