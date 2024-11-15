package eddie.energy.europeanmasterdata;

public record MeteredDataAdministrator(
        String country,
        String company,
        String companyId,
        String websiteUrl,
        String officialContact,
        String permissionAdministrator
) {
}
