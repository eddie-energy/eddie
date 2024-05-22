package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDto;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;

public record IdentifiableAccountingPointDetails(
        DkEnerginetPermissionRequest permissionRequest,
        MeteringPointDetailsCustomerDto meteringPointDetails
) {
}
