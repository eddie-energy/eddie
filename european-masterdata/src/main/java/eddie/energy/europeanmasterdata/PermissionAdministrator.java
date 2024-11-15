package eddie.energy.europeanmasterdata;

public record PermissionAdministrator(
        String country,
        String company,
        String name,
        String companyId,
        String jumpOffUrl,
        String regionConnector
) {
}
