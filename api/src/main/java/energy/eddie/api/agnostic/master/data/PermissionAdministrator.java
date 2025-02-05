package energy.eddie.api.agnostic.master.data;

public record PermissionAdministrator(
        String country,
        String company,
        String name,
        String companyId,
        String jumpOffUrl,
        String regionConnector
) {
}
