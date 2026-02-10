package energy.eddie.regionconnector.aiida.mqtt.message.processor;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.exceptions.PermissionInvalidException;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneId;

public abstract class BaseMessageProcessor implements AiidaMessageProcessor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper;
    private final AiidaPermissionRequestViewRepository permissionRequestViewRepository;

    protected BaseMessageProcessor(AiidaPermissionRequestViewRepository permissionRequestViewRepository,
                                   ObjectMapper objectMapper
    ) {
        this.permissionRequestViewRepository = permissionRequestViewRepository;
        this.objectMapper = objectMapper;
    }

    protected final AiidaPermissionRequest getAndValidatePermissionRequest(
            String permissionId
    ) throws PermissionNotFoundException, PermissionInvalidException {
        var permissionRequest = permissionRequestViewRepository
                .findByPermissionId(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        validatePermissionRequest(permissionRequest);

        return permissionRequest;
    }

    private void validatePermissionRequest(AiidaPermissionRequest permissionRequest) throws PermissionInvalidException {
        validateStatus(permissionRequest);
        validateTimespan(permissionRequest);
    }

    private void validateStatus(AiidaPermissionRequest permissionRequest) throws PermissionInvalidException {
        if (permissionRequest.status() != PermissionProcessStatus.ACCEPTED) {
            throw new PermissionInvalidException(
                    permissionRequest.permissionId(),
                    "Permission status is not ACCEPTED but %s".formatted(permissionRequest.status())
            );
        }
    }

    private void validateTimespan(AiidaPermissionRequest permissionRequest) throws PermissionInvalidException {
        var now = LocalDate.now(ZoneId.systemDefault());

        if (now.isBefore(permissionRequest.start()) || now.isAfter(permissionRequest.end())) {
            throw new PermissionInvalidException(
                    permissionRequest.permissionId(),
                    "Current date is outside of permission timespan (%s - %s)".formatted(permissionRequest.start(),
                                                                                         permissionRequest.end())
            );
        }
    }
}
