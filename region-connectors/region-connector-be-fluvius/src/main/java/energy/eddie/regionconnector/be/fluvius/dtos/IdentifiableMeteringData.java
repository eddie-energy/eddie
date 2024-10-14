package energy.eddie.regionconnector.be.fluvius.dtos;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.regionconnector.be.fluvius.client.model.ElectricityMeterResponseModel;

import java.util.List;

public record IdentifiableMeteringData(
        PermissionRequest permissionRequest,
        List<ElectricityMeterResponseModel> meteredData
) implements IdentifiablePayload<PermissionRequest, List<ElectricityMeterResponseModel>> {

    @Override
    public List<ElectricityMeterResponseModel> payload() {
        return meteredData;
    }
}
