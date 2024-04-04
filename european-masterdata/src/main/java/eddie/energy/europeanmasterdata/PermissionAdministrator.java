package eddie.energy.europeanmasterdata;

public record PermissionAdministrator(
        String country,
        String company,
        String companyId,
        String jumpOffUrl,
        String regionConnector
) {
}
