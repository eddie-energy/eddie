// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.agnostic;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public record IdentifiableAccountingPointData(
        EsPermissionRequest permissionRequest,
        AccountingPointData accountingPointData
) implements IdentifiablePayload<EsPermissionRequest, AccountingPointData> {
    @Override
    public AccountingPointData payload() {
        return accountingPointData;
    }
}
