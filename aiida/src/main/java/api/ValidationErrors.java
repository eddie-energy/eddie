package api;

import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;


public class ValidationErrors implements ErrorMapper {

    private final MethodArgumentNotValidException exception;

    public ValidationErrors(MethodArgumentNotValidException exception) {
        this.exception = exception;
    }

    @Override
    public List<EddieApiError> asErrorsList() {
        return exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(this::mapObjectErrorToErrorMessage)
                .toList();
    }

    private EddieApiError mapObjectErrorToErrorMessage(ObjectError error) {
        String fieldName;

        if (error instanceof FieldError fieldError) {
            fieldName = fieldError.getField();
        } else {
            fieldName = error.getObjectName();
        }
        return new EddieApiError("%s: %s".formatted(fieldName, error.getDefaultMessage()));
    }
}
