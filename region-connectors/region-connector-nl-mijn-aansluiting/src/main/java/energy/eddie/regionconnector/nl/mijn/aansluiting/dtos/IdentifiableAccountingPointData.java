package energy.eddie.regionconnector.nl.mijn.aansluiting.dtos;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoint;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;

import java.util.List;

public record IdentifiableAccountingPointData(
        MijnAansluitingPermissionRequest permissionRequest,
        List<MeteringPoint> payload
) implements IdentifiablePayload<MijnAansluitingPermissionRequest, List<MeteringPoint>> {

}
