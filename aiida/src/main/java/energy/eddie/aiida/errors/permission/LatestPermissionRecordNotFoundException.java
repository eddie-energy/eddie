package energy.eddie.aiida.errors.permission;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LatestPermissionRecordNotFoundException extends Exception {
    public LatestPermissionRecordNotFoundException(UUID permissionId) {
        super("Latest permission record not found for permission: %s".formatted(permissionId));
    }
}
