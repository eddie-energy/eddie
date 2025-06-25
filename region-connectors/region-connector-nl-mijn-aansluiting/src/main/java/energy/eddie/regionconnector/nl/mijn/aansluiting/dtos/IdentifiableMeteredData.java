package energy.eddie.regionconnector.nl.mijn.aansluiting.dtos;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;

import java.util.List;

public record IdentifiableMeteredData(MijnAansluitingPermissionRequest permissionRequest,
                                      List<MijnAansluitingResponse> meteredData)
        implements IdentifiablePayload<MijnAansluitingPermissionRequest, List<MijnAansluitingResponse>> {
    @Override
    public List<MijnAansluitingResponse> payload() {
        return meteredData;
    }
}
