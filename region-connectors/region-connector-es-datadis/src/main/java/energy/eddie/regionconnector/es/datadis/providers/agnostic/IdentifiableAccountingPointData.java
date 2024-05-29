package energy.eddie.regionconnector.es.datadis.providers.agnostic;

import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public record IdentifiableAccountingPointData(
        EsPermissionRequest permissionRequest,
        AccountingPointData accountingPointData
) {}
