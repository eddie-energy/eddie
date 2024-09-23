package energy.eddie.regionconnector.nl.mijn.aansluiting.dtos;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.nl.mijn.aansluiting.api.NlPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.ConsumptionData;

import java.util.List;

public record IdentifiableAccountingPointData(NlPermissionRequest permissionRequest,
                                              List<ConsumptionData> payload) implements IdentifiablePayload<NlPermissionRequest, List<ConsumptionData>> {
}
