package energy.eddie.aiida.errors;

public class PermissionUnfulfillableException extends Exception {
    private final String serviceName;

    public PermissionUnfulfillableException(String serviceName) {this.serviceName = serviceName;}

    public String serviceName() {
        return serviceName;
    }
}
