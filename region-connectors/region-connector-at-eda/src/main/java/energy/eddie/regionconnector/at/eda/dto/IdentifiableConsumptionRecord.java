package energy.eddie.regionconnector.at.eda.dto;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;

import java.util.List;

public record IdentifiableConsumptionRecord(ConsumptionRecord consumptionRecord,
                                            List<AtPermissionRequest> permissionRequests) {
}