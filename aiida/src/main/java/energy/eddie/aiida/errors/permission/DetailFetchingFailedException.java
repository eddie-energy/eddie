package energy.eddie.aiida.errors.permission;

import java.util.UUID;

public class DetailFetchingFailedException extends Exception {
    public DetailFetchingFailedException(UUID permissionId) {
        super("Failed to fetch permission details or MQTT credentials for permission '%s'".formatted(permissionId));
    }
}
