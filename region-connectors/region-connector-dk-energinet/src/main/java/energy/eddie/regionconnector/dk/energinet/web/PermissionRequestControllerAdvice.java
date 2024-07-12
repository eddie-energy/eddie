package energy.eddie.regionconnector.dk.energinet.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.regionconnector.dk.energinet.services.InvalidRefreshTokenException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;

@RestControllerAdvice
public class PermissionRequestControllerAdvice {
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleInvalidRefreshTokenException() {
        var errors = Map.of(
                ERRORS_PROPERTY_NAME,
                List.of(new EddieApiError(
                        "Refresh Token is either malformed or is not valid until the end of the requested permission"
                ))
        );
        return ResponseEntity.badRequest()
                             .body(errors);
    }
}
