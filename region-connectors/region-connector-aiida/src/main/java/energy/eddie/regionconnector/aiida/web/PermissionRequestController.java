package energy.eddie.regionconnector.aiida.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.dtos.PermissionUpdateDto;
import energy.eddie.regionconnector.aiida.dtos.QrCodeDto;
import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import energy.eddie.regionconnector.aiida.mqtt.MqttDto;
import energy.eddie.regionconnector.aiida.services.AiidaPermissionService;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriTemplate;

import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;

@RestController
public class PermissionRequestController {
    public static final String PATH_UPDATE_PERMISSION_REQUEST = PATH_PERMISSION_REQUEST + "/{permissionId}";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestController.class);
    private final AiidaPermissionService permissionService;

    @Autowired
    public PermissionRequestController(AiidaPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping(value = PATH_PERMISSION_REQUEST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QrCodeDto> createPermissionRequest(
            @Valid @RequestBody PermissionRequestForCreation permissionRequestForCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, JwtCreationFailedException {
        var qrCodeDto = permissionService.createValidateAndSendPermissionRequest(permissionRequestForCreation);

        var location = new UriTemplate(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)
                .expand(qrCodeDto.permissionId());

        return ResponseEntity.created(location).body(qrCodeDto);
    }

    @PatchMapping(value = PATH_UPDATE_PERMISSION_REQUEST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> updatePermissionRequest(
            @PathVariable String permissionId,
            @Valid @RequestBody PermissionUpdateDto updateDto
    ) throws PermissionNotFoundException, CredentialsAlreadyExistException, PermissionStateTransitionException {
        LOGGER.info("Got request to update permission {} with DTO {}", permissionId, updateDto);

        switch (updateDto.operation()) {
            case ACCEPT -> {
                MqttDto body = permissionService.acceptPermission(permissionId);
                return ResponseEntity.ok(body);
            }
            case REJECT -> {
                permissionService.rejectPermission(permissionId);
                return ResponseEntity.noContent().build();
            }
            case UNFULFILLABLE -> {
                permissionService.unableToFulFillPermission(permissionId);
                return ResponseEntity.noContent().build();
            }
            default -> {
                LOGGER.warn("Operation {} is not supported", updateDto.operation());
                return ResponseEntity.badRequest()
                                     .body(Map.of(ERRORS_PROPERTY_NAME,
                                                  List.of(new EddieApiError("Operation not supported"))));
            }
        }
    }
}
