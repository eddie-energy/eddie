// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDto;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;

public record IdentifiableAccountingPointDetails(
        DkEnerginetPermissionRequest permissionRequest,
        MeteringPointDetailsCustomerDto meteringPointDetails
) implements IdentifiablePayload<DkEnerginetPermissionRequest, MeteringPointDetailsCustomerDto> {
    @Override
    public MeteringPointDetailsCustomerDto payload() {
        return meteringPointDetails();
    }
}
