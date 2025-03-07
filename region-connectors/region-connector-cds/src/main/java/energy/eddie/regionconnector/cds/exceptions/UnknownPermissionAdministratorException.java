package energy.eddie.regionconnector.cds.exceptions;

public class UnknownPermissionAdministratorException extends Exception {
    public UnknownPermissionAdministratorException(Long cdsServerId) {
        super("Unknown permission administrator: " + cdsServerId);
    }
}
