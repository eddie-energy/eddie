package energy.eddie.regionconnector.es.datadis.providers.agnostic;

import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

public record IdentifiableMeteringData(
        EsPermissionRequest permissionRequest,
        IntermediateMeteringData intermediateMeteringData
) {
}
