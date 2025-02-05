package energy.eddie.api.agnostic.master.data;

public record MeteredDataAdministrator(
        String country,
        String company,
        String companyId,
        String websiteUrl,
        String officialContact,
        String permissionAdministrator
) {
}
