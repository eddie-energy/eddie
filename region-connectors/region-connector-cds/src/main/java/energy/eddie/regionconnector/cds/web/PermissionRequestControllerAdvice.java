package energy.eddie.regionconnector.cds.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.GlobalConfig;
import energy.eddie.regionconnector.cds.exceptions.UnknownPermissionAdministratorException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackageClasses = PermissionRequestController.class)
public class PermissionRequestControllerAdvice {
    @ExceptionHandler(UnknownPermissionAdministratorException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleUnknownPermissionAdministratorException(
            UnknownPermissionAdministratorException exception
    ) {
        var errorMap = Map.of(
                GlobalConfig.ERRORS_PROPERTY_NAME,
                List.of(new EddieApiError(exception.getMessage()))
        );
        return ResponseEntity.badRequest().body(errorMap);
    }
}
