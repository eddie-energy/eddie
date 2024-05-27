package energy.eddie.aiida.errors;

public class DetailFetchingFailedException extends Exception {
    public DetailFetchingFailedException(String permissionId) {
        super("Failed to fetch permission details or MQTT credentials for permission '%s' from EDDIE framework".formatted(
                permissionId));
    }
}
