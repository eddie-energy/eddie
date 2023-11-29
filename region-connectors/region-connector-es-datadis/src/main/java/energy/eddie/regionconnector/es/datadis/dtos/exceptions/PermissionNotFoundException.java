package energy.eddie.regionconnector.es.datadis.dtos.exceptions;

public class PermissionNotFoundException extends Exception {
    public PermissionNotFoundException(String permissionId) {
        super("No permission with ID %s found".formatted(permissionId));
    }
}
