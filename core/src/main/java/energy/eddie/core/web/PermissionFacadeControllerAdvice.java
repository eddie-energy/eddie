package energy.eddie.core.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.core.services.UnknownRegionConnectorException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;

@RestControllerAdvice
public class PermissionFacadeControllerAdvice {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionFacadeControllerAdvice.class);

    @ExceptionHandler(value = {DataNeedNotFoundException.class, UnknownRegionConnectorException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleUnknownRegionConnectorAndDataNeedNotFoundException(
            Exception exception
    ) {
        LOGGER.info("Exception occurred while trying to calculate data need information", exception);
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }
}
