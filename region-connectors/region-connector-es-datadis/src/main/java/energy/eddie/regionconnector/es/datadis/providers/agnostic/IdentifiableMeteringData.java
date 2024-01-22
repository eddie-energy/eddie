package energy.eddie.regionconnector.es.datadis.providers.agnostic;

import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

import java.util.List;

public record IdentifiableMeteringData(
        EsPermissionRequest permissionRequest,
        List<MeteringData> meteringData) {
}
